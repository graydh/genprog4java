/*
 * Copyright (c) 2014-2015, 
 *  Claire Le Goues     <clegoues@cs.cmu.edu>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package clegoues.genprog4java.Fitness;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnitTestRunner
{
	public static void main(String[] args)
	{
		try {
			String clazzName = args[0].trim();
			System.err.println("Test Class: " + clazzName);
			Class<?>[] testClazz = new Class[1];
			testClazz[0] = Class.forName(clazzName);

			Request testRequest = Request.classes(testClazz);
			System.out.println("Requested #: " + testRequest.getRunner().testCount());

			JUnitCore runner = new JUnitCore();
			Result r = runner.run(testRequest);

			System.out.println("[SUCCESS]:"+r.wasSuccessful());
			System.out.println("[TOTAL]:"+r.getRunCount());
			System.out.println("[FAILURE]:"+r.getFailureCount());

			for(Failure f: r.getFailures())
			{
				System.out.println(f.toString());
				System.out.println(f.getTrace());
			}

			System.out.println("\n"+r.getFailures().toString());

		} catch (Exception e) {
			e.printStackTrace();
		}	
		Runtime.getRuntime().exit(0);
	}

}
// FIXME: do I need the listener?
class Listener extends RunListener {
	@Override
	public void testRunStarted(Description description) throws Exception {
		System.out.println("test started: " + description.toString());
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		System.out.println("test finished");
	}

	@Override
	public void testFinished(Description description) throws Exception {
		System.out.println("test: "+description.getDisplayName());
	}
}
