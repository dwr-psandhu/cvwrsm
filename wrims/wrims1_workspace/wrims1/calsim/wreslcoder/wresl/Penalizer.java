/*

Copyright (C) 1998, 2000 State of California, Department of Water Resources.

This program is licensed to you under the terms of the GNU General Public
License, version 2, as published by the Free Software Foundation.

You should have received a copy of the GNU General Public License along
with this program; if not, contact Dr. Sushil Arora, below, or the
Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.

THIS SOFTWARE AND DOCUMENTATION ARE PROVIDED BY THE CALIFORNIA DEPARTMENT
OF WATER RESOURCES AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE CALIFORNIA DEPARTMENT OF WATER RESOURCES OR ITS
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OR SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

For more information, contact:

Dr. Sushil Arora
California Dept. of Water Resources
Office of State Water Project Planning, Hydrology and Operations Section
1416 Ninth Street
Sacramento, CA  95814
916-653-7921
sushil@water.ca.gov

*/
package calsim.wreslcoder.wresl;
//import java.io.*;

/**
 * Handles the Wresl lhs<rhs and lhs>rhs expressions.  Each goal must create
 * two instances of this class, and use them to store the desired penalty values or
 * the fact that it is a hard constraint in one or both directions.
 *
 * @author Armin Munevar
 * @version $Id: Penalizer.java,v 1.1.2.3 2001/07/12 02:00:08 amunevar Exp $
 */
public class Penalizer {
    private String name;
    private String amount;

    /**
      * Creates a new penalty indicator that has a hard constraint.
      *
      */
    public Penalizer() {
       name = new String("constrain");
       amount = "0.0";
    }

    /**
      * Creates a new penalty indicator that has a penalty of the given amount.
      *
      * @param amt A constant equal to the desired penalty.
      */
    public Penalizer(float amt) {
       name = "penalty  ";   // must be nine chars
       amount = new String(new Float(amt).toString());
    }

    /**
      * Creates a new penalty indicator that has a penalty of the given amount.
      *
      * @param amt A String expression that will later evaluate to the desired penalty.
      */
    public Penalizer(String amt) {
       name = "penalty  ";   // must be nine chars
       amount = amt;
    }

    /**
      *  Revise this penalty indicator to have the specified penalty expression
      *
      * @param amt A String expression that will later evaluate to the desired penalty.
      */
    public void newPenalty(String amt) {
       name = "penalty  ";   // must be nine chars
       amount = amt;
    }

    /**
      *  Returns a <code>String</code> representation of this penalty expresion.
      *  @return Such a string.
      */
    public String asString() {
       return new String(name + amount);
    }
}
