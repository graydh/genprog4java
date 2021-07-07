package tests;

import static org.junit.Assert.assertTrue;
import flakyExample.RandomExample;

import org.junit.Test;

public class flakyExampleTestsPos {

  @Test
  public void testGive() {
	  
    RandomExample tester = new RandomExample();

    // Flaky
    assertTrue("Should be greater than 0.0", tester.give() > 0.2);
  }

} 