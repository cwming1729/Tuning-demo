package tuning;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@RunWith(JUnit4ClassRunner.class)
public class TuningDemo4 {

  public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
  public static final char MIN_LOW_SURROGATE  = '\uDC00';
  public static final char MAX_LOW_SURROGATE  = '\uDFFF';
  public static final char MIN_HIGH_SURROGATE = '\uD800';
  public static final char MAX_HIGH_SURROGATE = '\uDBFF';
  
  public static int toCodePoint(char high, char low) {
    return ((high << 10) + low) + (MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE << 10) - MIN_LOW_SURROGATE);
  }

  public static boolean isHighSurrogate(char ch) {
    return ch >= MIN_HIGH_SURROGATE && ch < (MAX_HIGH_SURROGATE + 1);
  }

  public static boolean isLowSurrogate(char ch) {
    return ch >= MIN_LOW_SURROGATE && ch < (MAX_LOW_SURROGATE + 1);
  }

  public static int codePointAt(String str, int index) {
    char[] value = str.toCharArray();
    if ((index < 0) || (index >= value.length)) {
      throw new StringIndexOutOfBoundsException(index);
    }
    return codePointAtImpl(value, index, value.length);
  }

  public static int codePointAtImpl(char[] a, int index, int limit) {
    char c1 = a[index++];
    if (isHighSurrogate(c1)) {
      if (index < limit) {
        char c2 = a[index];
        if (isLowSurrogate(c2)) {
          return toCodePoint(c1, c2);
        }
      }
    }
    return c1;
  }
  
  public static int codePointAt2(String str, int index) {
    char c1 = str.charAt(index++);
    if (isHighSurrogate(c1)) {
      if (index < str.length()) {
        char c2 = str.charAt(index);
        if (isLowSurrogate(c2)) {
          return toCodePoint(c1, c2);
        }
      }
    }
    return c1;
  }

  private static final int NUM_OF_TRIALS = 100_000;
  private static final int INPUT_STRING_LENGTH = 200;
  
  private static final List<String> INPUT_STRINGS;
  
  static {
    Random random = new Random();
    INPUT_STRINGS = IntStream
    .range(0, NUM_OF_TRIALS)
    .mapToObj(
      j ->
      {
        return random.ints(0, 65535)
        .limit(INPUT_STRING_LENGTH)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
      }
    ).collect(Collectors.toList());
  }

  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testCodePointAtOld() {
    BigDecimal sum = BigDecimal.ZERO;
    for (String input : INPUT_STRINGS) {
      for (int i = 0; i < input.length(); i++) {
        int j = codePointAt(input, i);
        sum = sum.add(new BigDecimal(j));
      }
    }
//    System.out.println("Sum of code point = " + sum);
  }
  
  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testCodePointAtNew() {
    BigDecimal sum = BigDecimal.ZERO;
    for (String input : INPUT_STRINGS) {
      for (int i = 0; i < input.length(); i++) {
        int j = codePointAt2(input, i);
        sum = sum.add(new BigDecimal(j));
      }
    }
//    System.out.println("Sum of code point = " + sum);
  }

  @Test
  public void testCorrectness() {

    // assert the output of both method is the same for each input in INPUT_STRINGS
    for (String input : INPUT_STRINGS) {
      for (int i = 0; i < input.length(); i++) {
        int j = codePointAt2(input, i);
        int k = codePointAt(input, i);
        assertEquals(j, k);
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    Options opts = new OptionsBuilder()
        .include(".*")
        .warmupIterations(1)
        .measurementIterations(5)
        .forks(1)
        .build();

    new Runner(opts).run();
  }
  
  
}