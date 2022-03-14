package tuning;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
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
public class TuningDemo1 {
  
  private static final int NUM_OF_TRIALS = 1000;
  private static final int INPUT_STRING_LENGTH = 5000;
  private static final List<String> INPUT_STRINGS;

  static {
    
    {
      System.out.println("Initializing test data");
      long start = System.currentTimeMillis();
      Random random = new Random();
      
      INPUT_STRINGS = 
      IntStream
      .range(0, NUM_OF_TRIALS)
      .mapToObj(
        j ->
        {
          return random.ints(10, 127)
          .filter(i -> i == 10 || i == 13 || (i >= 32 && i < 127))
          .limit(INPUT_STRING_LENGTH)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
        }
      ).collect(Collectors.toList());
      System.out.println("Initialized test data in " + (System.currentTimeMillis() - start) +  " ms.");
    }
  }
  
  
  private void test(Function<String, String> testFunction, String name) {
    long start = System.currentTimeMillis();
    List<String> outputList = INPUT_STRINGS.stream().map(testFunction).collect(Collectors.toList());
//    System.out.println(name + " completed in " + (System.currentTimeMillis() - start) +  " ms.");
  }
  /**
   * Strategy A
   */
  public List<String> splitIntoList(String text, int size) {
    List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
    for (int start = 0; start < text.length(); start += size) {
      ret.add(text.substring(start, Math.min(text.length(), start + size)));
    }
    return ret;
  }

  public String getSqlCharStr(String inputString) {
      Integer splitSize = 500;
      String lineBreakReplacePattern = "#%#%#";
      if (inputString == null) {
          return null;
      }
      if (inputString.length() > splitSize) {
          String newStr = "";
          List<String> strs = splitIntoList(inputString, splitSize);
          for (String subStr : strs) {
              newStr += subStr.concat(lineBreakReplacePattern);
          }
          inputString = newStr;
      }
      inputString = inputString
          .replaceAll("'", "''")
          .replaceAll("&", "&'||'")
          .replaceAll("\r", "'||chr(13)||'")
          .replaceAll("\n", "'||chr(10)||'")
          .replaceAll(lineBreakReplacePattern, String.format("'%s||'", System.lineSeparator()));
      return String.format("'%s'", inputString);
  }
  
  /**
   * Strategy B
   */
  public String getSqlCharStr2(String str) {
    Integer size = 500;
    
    if (str == null) {
      return null;
    }
    StringBuffer output = new StringBuffer();
    for (int start = 0; start < str.length(); start += size) {
      String fragment = (str.substring(start, Math.min(str.length(), start + size)));
      
      output.append(fragment
        .replaceAll("'", "''")
        .replaceAll("&", "&'||'")
        .replaceAll("\r", "'||chr(13)||'")
        .replaceAll("\n", "'||chr(10)||'"));
      
      output.append(String.format("'%s||'", System.lineSeparator()));
    } // end for
    return output.toString();
  }
  
  /**
   * Strategy C
   */
  private static final String LINE = System.lineSeparator();
  private static final int SPLIT_SIZE = 500;
  
  public String getSqlCharStr3(String str) {

    if (str == null) {
        return null;
    }

    char[] chars = str.toCharArray();
    StringBuffer output = new StringBuffer(chars.length);
    output.append('\'');
    for (int i = 0; i < chars.length; i++) {
        if (i > 0 && i % SPLIT_SIZE == 0) {
            output.append("\'||" + LINE + "'");
        } 
        if (chars[i] == '\'') {
            output.append("\'\'");
        } else if (chars[i] == '&') {
            output.append("&\'||\'");
        } else if (chars[i] == '\r') {
            output.append("\'||chr(13)||\'");
        } else if (chars[i] == '\n') {
            output.append("\'||chr(10)||\'");
        } else {
            output.append(chars[i]);
        }
    }
    output.append('\'');
    return output.toString();
  }
  
  /**
   * Strategy D
   */
  
  private static String[] MAPPED_STRINGS;
  
  static {
    MAPPED_STRINGS = new String[65536];
    for (int i = 0; i < 65536; i++) {
      if (i == '\'') {
        MAPPED_STRINGS[i] = "\'\'";
      } else if (i == '&') {
        MAPPED_STRINGS[i] = "&\'||\'";
      } else if (i == '\r') {
        MAPPED_STRINGS[i] = "\'||chr(13)||\'";
      } else if (i == '\n') {
        MAPPED_STRINGS[i] = "\'||chr(10)||\'";
      } else {
        MAPPED_STRINGS[i] =  ("" + (char) i);
      }      
    }
  }
  
  public String getSqlCharStr4(String str) {
    if (str == null) {
      return null;
    }
    
    char[] chars = str.toCharArray();
    StringBuffer output = new StringBuffer(chars.length);
    output.append('\'');
    
    for (int i = 0; i < chars.length; i++) {
      if (i > 0 && i % SPLIT_SIZE == 0) {
        output.append("\'||" + LINE + "'");
      }
      output.append(MAPPED_STRINGS[(int) chars[i]]);
    }
    output.append('\'');
    return output.toString();
  }
  
  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testStrategyA() {
    test(this::getSqlCharStr, "Strategy A");
  }
  
  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testStrategyB() {
    test(this::getSqlCharStr2, "Strategy B");
  }
  
  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testStrategyC() {
    test(this::getSqlCharStr3, "Strategy C");
  }

  @Test
  @Benchmark
  @Fork(value = 1, warmups = 1)
  @BenchmarkMode(Mode.AverageTime)
  public void testStrategyD() {
    test(this::getSqlCharStr4, "Strategy D");
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
