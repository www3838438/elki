/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Lift interestingness measure.
 *
 * Reference:
 * <p>
 * S. Brin, R. Motwani, and C. Silverstein<br />
 * Beyond market baskets: Generalizing association rules to correlations<br />
 * In ACM SIGMOD Record, volume 26, ACM, 1997
 * </p>
 * 
 * @author Frederic Sautter
 */
@Reference(authors = "S. Brin, R. Motwani, and C. Silverstein", //
    title = "Beyond market baskets: Generalizing association rules to correlations", //
    booktitle = "ACM SIGMOD Record, volume 26, ACM, 1997")
public class Lift implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Lift() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return (sXY / (double) sX) / (sY / (double) t);
  }
}
