package io.github.giuseppecuiuri.snakesandladders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.CardDrawn;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DiceRolled;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DoubleSix;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.GameWon;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.LadderClimbed;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.PlayerMoved;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestAvoided;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestStarted;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SnakeSlid;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SpecialReached;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnSkipped;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnStarted;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameTest {

  /** The board of the worked examples: ladder 9 -> 31, snakes 17 -> 7 and 98 -> 79. */
  private static final GameConfig.Builder CLASSIC =
      GameConfig.builder(10, 10).ladder(9, 31).snake(17, 7).snake(98, 79);

  private final List<GameEvent> events = new ArrayList<>();
  private ScriptedRandom dice;

  private Game newGame(GameConfig config, int players, int... faces) {
    dice = new ScriptedRandom(faces);
    Game game = new Game(config, players, dice);
    game.addListener(events::add);
    return game;
  }

  private static Player first(Game game) {
    return game.players().get(0);
  }

  @Nested
  class GoldenExamples {

    @Test
    void sixThenSixThenFiveHitsTheSnakeThenTwoClimbsTheLadder() {
      Game game = newGame(CLASSIC.build(), 1, 1, 5, 2, 4, 2, 3, 1, 1);

      int[] positionAfterEachTurn = {6, 12, 7, 31};
      for (int expected : positionAfterEachTurn) {
        game.playTurn();
        assertThat(first(game).position()).isEqualTo(expected);
      }
    }

    @Test
    void fiveFrom97BouncesBackTo98AndSlidesDownTo79() {
      Game game = newGame(CLASSIC.build(), 1, 2, 3);
      first(game).moveTo(97);

      game.playTurn();
      assertThat(first(game).position()).isEqualTo(79);
      assertThat(events)
          .containsSubsequence(new PlayerMoved(1, 97, 98, true), new SnakeSlid(1, 98, 79));
    }
  }

  @Nested
  class BaseRules {

    @Test
    void eachTurnReportsWhatHappenedInOrder() {
      Game game = newGame(CLASSIC.build(), 1, 4, 5);

      game.playTurn();
      assertThat(events)
          .containsExactly(
              new TurnStarted(1, 1),
              new DiceRolled(1, List.of(4, 5)),
              new PlayerMoved(1, 0, 9, false),
              new LadderClimbed(1, 9, 31));
    }

    @Test
    void passingOverLadderFootOrSnakeHeadDoesNotMovePlayer() {
      Game game = newGame(CLASSIC.build(), 1, 2, 4, 3, 4);
      first(game).moveTo(5);

      game.playTurn(); // 5 + 6 = 11, past the ladder foot on 9
      assertThat(first(game).position()).isEqualTo(11);
      game.playTurn(); // 11 + 7 = 18, past the snake head on 17
      assertThat(first(game).position()).isEqualTo(18);
    }

    @Test
    void landingOnLadderTopOrSnakeTailDoesNotMovePlayer() {
      Game game = newGame(CLASSIC.build(), 1, 3, 4, 2, 4);

      game.playTurn(); // 0 + 7 = 7, the snake tail
      assertThat(first(game).position()).isEqualTo(7);
      first(game).moveTo(25);
      game.playTurn(); // 25 + 6 = 31, the ladder top
      assertThat(first(game).position()).isEqualTo(31);
    }

    @Test
    void excessPointsBounceBackFromTheLastCell() {
      Game game = newGame(CLASSIC.build(), 1, 2, 3);
      first(game).moveTo(96);

      game.playTurn(); // 96 + 5 = 101: one step past 100, back to 99
      assertThat(first(game).position()).isEqualTo(99);
      assertThat(events).contains(new PlayerMoved(1, 96, 99, true));
    }

    @Test
    void bounceOnSmallBoardResolvesTheCellReached() {
      Game game = newGame(GameConfig.builder(8, 6).snake(46, 30).build(), 1, 2, 3);
      first(game).moveTo(45);

      game.playTurn(); // 45 + 5 = 50: two past 48, back to 46, a snake head
      assertThat(first(game).position()).isEqualTo(30);
    }

    @Test
    void anExactRollToTheLastCellWinsAndEndsTheGame() {
      Game game = newGame(CLASSIC.build(), 2, 2, 3);
      first(game).moveTo(95);

      game.playTurn();
      assertThat(game.isOver()).isTrue();
      assertThat(game.winner()).contains(first(game));
      assertThat(game.currentPlayer()).isSameAs(first(game));
      assertThat(events).last().isEqualTo(new GameWon(1, 1));
      assertThatThrownBy(game::playTurn)
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("The game is over");
    }

    @Test
    void playersTakeTurnsInOrderAndTheOrderWrapsAround() {
      Game game = newGame(CLASSIC.build(), 2, 1, 1, 1, 2, 1, 3);

      game.playTurn();
      game.playTurn();
      game.playTurn();
      assertThat(game.players()).extracting(Player::position).containsExactly(6, 3);
      assertThat(game.turnsPlayed()).isEqualTo(3);
      assertThat(events)
          .filteredOn(TurnStarted.class::isInstance)
          .containsExactly(new TurnStarted(1, 1), new TurnStarted(2, 2), new TurnStarted(3, 1));
    }

    @Test
    void oneToSixPlayersCanPlay() {
      assertThat(new Game(CLASSIC.build(), 6, new ScriptedRandom()).players())
          .extracting(Player::id)
          .containsExactly(1, 2, 3, 4, 5, 6);
      assertThatThrownBy(() -> new Game(CLASSIC.build(), 0, new ScriptedRandom()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("players must be between 1 and 6, not 0");
      assertThatThrownBy(() -> new Game(CLASSIC.build(), 7, new ScriptedRandom()))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class DiceOptions {

    @Test
    void withOneDieEveryRollUsesOneDie() {
      Game game = newGame(GameConfig.builder(10, 10).dice(1).build(), 1, 4, 6);
      first(game).moveTo(10);

      game.playTurn();
      game.playTurn();
      assertThat(first(game).position()).isEqualTo(20);
      assertThat(events).filteredOn(DiceRolled.class::isInstance).hasSize(2);
    }

    @Test
    void singleDieNearTheEndStartsSixCellsBeforeTheLast() {
      GameConfig config = GameConfig.builder(10, 10).singleDieNearEnd(true).build();
      Game game = newGame(config, 1, 1, 2, 3);
      first(game).moveTo(91);

      game.playTurn(); // on 91 two dice: 1 + 2 = 3
      assertThat(first(game).position()).isEqualTo(94);
      game.playTurn(); // on 94, that is N - 6, one die
      assertThat(first(game).position()).isEqualTo(97);
      assertThat(dice.remaining()).isZero();
    }

    @Test
    void withoutSingleDieRuleTwoDiceAreRolledEverywhere() {
      Game game = newGame(CLASSIC.build(), 1, 1, 2);
      first(game).moveTo(94);

      game.playTurn();
      assertThat(first(game).position()).isEqualTo(97);
    }

    @Test
    void doubleSixRollsAgainAndMovesByTheNewRollOnly() {
      Game game = newGame(GameConfig.builder(10, 10).doubleSixBonus(true).build(), 1, 6, 6, 1, 2);
      first(game).moveTo(10);

      game.playTurn(); // 10 + 12 = 22, then 1 + 2 = 3
      assertThat(first(game).position()).isEqualTo(25);
      assertThat(events)
          .containsSubsequence(
              new PlayerMoved(1, 10, 22, false),
              new DoubleSix(1),
              new PlayerMoved(1, 22, 25, false));
    }

    @Test
    void bonusRollNearTheEndAlsoUsesOneDie() {
      GameConfig config =
          GameConfig.builder(10, 10).singleDieNearEnd(true).doubleSixBonus(true).build();
      Game game = newGame(config, 1, 6, 6, 3);
      first(game).moveTo(82);

      game.playTurn(); // 82 + 12 = 94, then a single die
      assertThat(first(game).position()).isEqualTo(97);
    }

    @Test
    void doubleSixThatWinsEndsTheTurnWithoutBonusRoll() {
      Game game = newGame(GameConfig.builder(10, 10).doubleSixBonus(true).build(), 1, 6, 6, 1, 2);
      first(game).moveTo(88);

      game.playTurn();
      assertThat(first(game).position()).isEqualTo(100);
      assertThat(game.isOver()).isTrue();
      assertThat(dice.remaining()).isEqualTo(2);
    }

    @Test
    void withoutDoubleSixRuleItIsAnOrdinaryRoll() {
      Game game = newGame(CLASSIC.build(), 1, 6, 6);
      first(game).moveTo(10);

      game.playTurn();
      assertThat(first(game).position()).isEqualTo(22);
      assertThat(events).noneMatch(DoubleSix.class::isInstance);
    }
  }

  @Nested
  class RestCells {

    private final GameConfig config =
        GameConfig.builder(10, 10).special(14, Special.BENCH).special(24, Special.INN).build();

    @Test
    void theBenchSkipsOneTurnAndTheInnThree() {
      Game game = newGame(config, 1, 1, 3, 4, 6, 1, 1);
      first(game).moveTo(10);

      game.playTurn(); // 10 + 4 = 14, the bench
      game.playTurn(); // skipped
      assertThat(dice.remaining()).isEqualTo(4);
      game.playTurn(); // 14 + 10 = 24, the inn
      game.playTurn();
      game.playTurn();
      game.playTurn(); // three turns skipped
      assertThat(dice.remaining()).isEqualTo(2);
      game.playTurn(); // 24 + 2
      assertThat(first(game).position()).isEqualTo(26);
      assertThat(events)
          .containsSubsequence(
              new SpecialReached(1, 14, Special.BENCH),
              new RestStarted(1, 1),
              new TurnSkipped(1, 0),
              new SpecialReached(1, 24, Special.INN),
              new RestStarted(1, 3),
              new TurnSkipped(1, 2),
              new TurnSkipped(1, 1),
              new TurnSkipped(1, 0));
    }

    @Test
    void theOtherPlayersKeepPlayingWhileOneRests() {
      Game game = newGame(config, 2, 1, 3, 1, 1, 1, 1, 2, 2);
      first(game).moveTo(10);

      game.playTurn(); // player 1: 10 + 4 = 14, the bench
      game.playTurn(); // player 2: 0 + 2
      game.playTurn(); // player 1 rests
      game.playTurn(); // player 2: 2 + 2
      game.playTurn(); // player 1: 14 + 4
      assertThat(game.players()).extracting(Player::position).containsExactly(18, 4);
    }

    @Test
    void doubleSixEndingOnRestCellLosesItsBonusRoll() {
      GameConfig withBonus =
          GameConfig.builder(10, 10).doubleSixBonus(true).special(22, Special.INN).build();
      Game game = newGame(withBonus, 1, 6, 6, 1, 2);
      first(game).moveTo(10);

      game.playTurn(); // 10 + 12 = 22, the inn: no bonus roll
      assertThat(first(game).position()).isEqualTo(22);
      assertThat(first(game).restTurns()).isEqualTo(3);
      assertThat(dice.remaining()).isEqualTo(2);
    }
  }

  @Nested
  class BonusCells {

    @Test
    void theSpringMovesOnceMoreByTheLastRoll() {
      Game game = newGame(GameConfig.builder(10, 10).special(20, Special.SPRING).build(), 1, 2, 4);
      first(game).moveTo(14);

      game.playTurn(); // 14 + 6 = 20, the spring adds the same 6
      assertThat(first(game).position()).isEqualTo(26);
    }

    @Test
    void theDiceCellRollsAgainAndResolvesTheCellReached() {
      GameConfig config =
          GameConfig.builder(10, 10).special(20, Special.DICE).ladder(22, 40).build();
      Game game = newGame(config, 1, 2, 4, 1, 1);
      first(game).moveTo(14);

      game.playTurn(); // 14 + 6 = 20, roll again 1 + 1 = 2: 22 is a ladder foot
      assertThat(first(game).position()).isEqualTo(40);
    }

    @Test
    void theSpringCanLeadOntoSnakeHead() {
      GameConfig config =
          GameConfig.builder(10, 10).special(95, Special.SPRING).snake(97, 70).build();
      Game game = newGame(config, 1, 1, 1);
      first(game).moveTo(93);

      game.playTurn(); // 93 + 2 = 95, the spring adds 2: 97 is a snake head
      assertThat(first(game).position()).isEqualTo(70);
    }

    @Test
    void springThatBouncesBackOntoItselfEndsTheTurn() {
      Game game = newGame(GameConfig.builder(10, 10).special(97, Special.SPRING).build(), 1, 1, 5);
      first(game).moveTo(91);

      game.playTurn(); // 91 + 6 = 97, the spring: 97 + 6 = 103, back to 97, where it already fired
      assertThat(first(game).position()).isEqualTo(97);
      assertThat(events).filteredOn(SpecialReached.class::isInstance).hasSize(1);
    }

    @Test
    void doubleSixRolledOnTheDiceCellAlsoEarnsTheBonus() {
      GameConfig config =
          GameConfig.builder(10, 10).doubleSixBonus(true).special(20, Special.DICE).build();
      Game game = newGame(config, 1, 2, 4, 6, 6, 1, 1);
      first(game).moveTo(14);

      game.playTurn(); // 14 + 6 = 20, the dice cell: 6 + 6 = 12 to 32, bonus 1 + 1 to 34
      assertThat(first(game).position()).isEqualTo(34);
      assertThat(events).contains(new DoubleSix(1));
    }
  }

  @Nested
  class DrawCards {

    private final GameConfig config =
        GameConfig.builder(10, 10)
            .special(5, Special.DRAW_CARD)
            .deck(List.of(Card.BENCH, Card.SPRING, Card.INN, Card.DICE))
            .build();

    @Test
    void cardsAreDealtFromTheTopAndGoBackToTheBottom() {
      Game game = newGame(config, 5, 1, 3, 1, 3, 1, 3, 1, 3, 2, 2, 1, 3);
      for (Player player : game.players()) {
        player.moveTo(1);
      }

      for (int turn = 0; turn < 5; turn++) {
        game.playTurn(); // each player rolls 4 from cell 1 to the draw cell on 5
      }
      assertThat(events)
          .filteredOn(CardDrawn.class::isInstance)
          .extracting(event -> ((CardDrawn) event).card())
          .containsExactly(Card.BENCH, Card.SPRING, Card.INN, Card.DICE, Card.BENCH);
    }

    @Test
    void eachCardHasTheEffectOfTheMatchingCell() {
      Game game = newGame(config, 5, 1, 3, 1, 3, 1, 3, 1, 3, 2, 2, 1, 3);
      for (Player player : game.players()) {
        player.moveTo(1);
      }

      for (int turn = 0; turn < 5; turn++) {
        game.playTurn();
      }
      assertThat(game.players()).extracting(Player::position).containsExactly(5, 9, 5, 9, 5);
      assertThat(game.players()).extracting(Player::restTurns).containsExactly(1, 0, 3, 0, 1);
    }

    @Test
    void theDeckIsShuffledOnceWithTheGamesRandomGenerator() {
      GameConfig bigDeck =
          GameConfig.builder(10, 10)
              .special(5, Special.DRAW_CARD)
              .deck(
                  List.of(
                      Card.BENCH,
                      Card.BENCH,
                      Card.INN,
                      Card.INN,
                      Card.DICE,
                      Card.DICE,
                      Card.SPRING,
                      Card.SPRING))
              .build();

      assertThat(drawOrder(bigDeck, 42)).isEqualTo(drawOrder(bigDeck, 42));
      assertThat(drawOrder(bigDeck, 42)).isNotEqualTo(drawOrder(bigDeck, 7));
    }

    private List<Card> drawOrder(GameConfig config, long seed) {
      List<Card> drawn = new ArrayList<>();
      Game game = new Game(config, 6, new SplittableRandom(seed));
      game.addListener(
          event -> {
            if (event instanceof CardDrawn(int player, Card card)) {
              drawn.add(card);
            }
          });
      while (!game.isOver() && game.turnsPlayed() < 300) {
        game.playTurn();
      }
      return drawn.subList(0, Math.min(8, drawn.size()));
    }
  }

  @Nested
  class NoRestCard {

    @Test
    void keptCardCancelsAnInnStayAndGoesBackToTheBottom() {
      GameConfig config =
          GameConfig.builder(10, 10)
              .special(5, Special.DRAW_CARD)
              .special(9, Special.INN)
              .deck(List.of(Card.NO_REST, Card.BENCH))
              .build();
      Game game = newGame(config, 1, 1, 3, 2, 2, 1, 1);
      first(game).moveTo(1);

      game.playTurn(); // 1 + 4 = 5: draws the no-rest card and keeps it
      assertThat(first(game).noRestCards()).isOne();
      game.playTurn(); // 5 + 4 = 9: the inn, cancelled by the card
      assertThat(first(game).noRestCards()).isZero();
      assertThat(first(game).restTurns()).isZero();
      game.playTurn(); // so this turn is played: 9 + 2
      assertThat(first(game).position()).isEqualTo(11);
      assertThat(events)
          .containsSubsequence(
              new CardDrawn(1, Card.NO_REST),
              new SpecialReached(1, 9, Special.INN),
              new RestAvoided(1));
    }

    @Test
    void anInnCardGoesBackToTheBottomAfterEachDraw() {
      GameConfig config =
          GameConfig.builder(10, 10)
              .special(5, Special.DRAW_CARD)
              .deck(List.of(Card.NO_REST, Card.INN))
              .build();
      Game game = newGame(config, 3, 1, 3, 1, 3, 1, 3);
      for (Player player : game.players()) {
        player.moveTo(1);
      }

      game.playTurn(); // player 1 keeps the no-rest card; the deck is now: inn
      game.playTurn(); // player 2 draws the inn card, which goes back to the bottom
      game.playTurn(); // player 3 draws the same inn card again
      assertThat(game.players()).extracting(Player::restTurns).containsExactly(0, 3, 3);
    }

    @Test
    void noRestCardAlsoCancelsRestFromDrawnCard() {
      GameConfig config =
          GameConfig.builder(10, 10)
              .special(5, Special.DRAW_CARD)
              .deck(List.of(Card.NO_REST, Card.INN))
              .build();
      Game game = newGame(config, 1, 1, 3, 1, 3);
      first(game).moveTo(1);

      game.playTurn(); // 1 + 4 = 5: keeps the no-rest card
      first(game).moveTo(1);
      game.playTurn(); // 1 + 4 = 5: draws the inn card, and the no-rest card cancels it
      assertThat(first(game).restTurns()).isZero();
      assertThat(events).containsSubsequence(new CardDrawn(1, Card.INN), new RestAvoided(1));
    }

    @Test
    void playersCanKeepSeveralNoRestCards() {
      GameConfig config =
          GameConfig.builder(10, 10)
              .special(5, Special.DRAW_CARD)
              .special(15, Special.DRAW_CARD)
              .deck(List.of(Card.NO_REST, Card.NO_REST, Card.BENCH))
              .build();
      Game game = newGame(config, 1, 1, 3, 4, 6);
      first(game).moveTo(1);

      game.playTurn(); // 1 + 4 = 5: first no-rest card
      game.playTurn(); // 5 + 10 = 15: second no-rest card
      assertThat(first(game).noRestCards()).isEqualTo(2);
    }

    @Test
    void doubleSixKeepsItsBonusWhenTheCardCancelsTheRest() {
      GameConfig config =
          GameConfig.builder(10, 10)
              .doubleSixBonus(true)
              .special(5, Special.DRAW_CARD)
              .special(22, Special.INN)
              .deck(List.of(Card.NO_REST, Card.BENCH))
              .build();
      Game game = newGame(config, 1, 1, 3, 6, 6, 1, 2);
      first(game).moveTo(1);

      game.playTurn(); // 1 + 4 = 5: keeps the no-rest card
      first(game).moveTo(10);
      game.playTurn(); // 10 + 12 = 22: the inn is cancelled, so the bonus roll 1 + 2 is played
      assertThat(first(game).position()).isEqualTo(25);
    }

    @Test
    void deckNeedsOneCardOtherThanTheNoRestCard() {
      assertThatThrownBy(
              () ->
                  GameConfig.builder(10, 10)
                      .special(5, Special.DRAW_CARD)
                      .deck(List.of(Card.NO_REST))
                      .build())
          .hasMessageContaining("the deck needs at least one card other than the no-rest card");
      assertThat(Card.standardDeck(true)).endsWith(Card.NO_REST);
    }
  }
}
