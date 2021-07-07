package tests;

import static org.junit.Assert.assertTrue;
import flakyExample.RandomExample;

import org.junit.Test;

public class flakyExampleTestsNeg {

  @Test
  public void testGive() {

    RandomExample tester = new RandomExample();

    // assert statements
    assertTrue("Should be greater than 1", tester.give() > 1.0); 
 
   }

} 