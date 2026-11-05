package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.random.RandomGenerator;

/** Returns scripted die faces, so that a test decides every roll. */
final class ScriptedRandom implements RandomGenerator {

  private final Deque<Integer> faces = new ArrayDeque<>();

  ScriptedRandom(int... faces) {
    for (int face : faces) {
      this.faces.add(face);
    }
  }

  @Override
  public int nextInt(int origin, int bound) {
    if (origin != 1 || bound != 7) {
      throw new IllegalArgumentException("Not a die roll: " + origin + " to " + bound);
    }
    Integer face = faces.poll();
    if (face == null) {
      throw new IllegalStateException(
          "The dice script ran out: more dice were rolled than expected");
    }
    return face;
  }

  /** Shuffles call this; returning the last index leaves the deck in its configured order. */
  @Override
  public int nextInt(int bound) {
    return bound - 1;
  }

  @Override
  public long nextLong() {
    throw new UnsupportedOperationException("Only die rolls are scripted");
  }

  int remaining() {
    return faces.size();
  }
}
