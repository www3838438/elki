package de.lmu.ifi.dbs.elki.index.projected;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.LngLatToECEFProjection;
import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProjectedView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LngLatDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Index a 2d data set (consisting of Lng/Lat pairs) by using a projection to 3D
 * coordinates (WGS-86 to ECEF).
 * 
 * Earth-Centered, Earth-Fixed (ECEF) is a 3D coordinate system, sometimes also
 * referred to as XYZ, that uses 3 cartesian axes. The center is at the earths
 * center of mass, the z axis points to the north pole. X axis is to the prime
 * meridan at the equator (so latitude 0, longitude 0), and the Y axis is
 * orthogonal going to the east (latitude 0, longitude 90°E).
 * 
 * The Euclidean distance in this coordinate system is a lower bound for the
 * great-circle distance, and Euclidean coordinates are supposedly easier to
 * index.
 * 
 * Note: this index will <b>only</b> support the distance function
 * {@link LngLatDistanceFunction}, as it uses a projection that will map data
 * according to this great circle distance. If the query hint
 * {@link DatabaseQuery#HINT_EXACT} is set, it will not be used.
 * 
 * TODO: add support for a flag "refine" that computes the correct distances,
 * but only for the candidates returned.
 * 
 * This way of indexing geo data in regular databases was discussed in:
 * 
 * Reference:
 * <p>
 * Geodetic point-in-polygon query processing in oracle spatial<br />
 * Hu, Ying and Ravada, Siva and Anderson, Richard<br />
 * Advances in Spatial and Temporal Databases, SSTD 2011
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Reference(title = "Geodetic point-in-polygon query processing in oracle spatial", authors = "Hu, Ying and Ravada, Siva and Anderson, Richard", booktitle = "Advances in Spatial and Temporal Databases, SSTD 2011")
public class LngLatAsECEFIndex<O extends NumberVector<?>> extends ProjectedIndex<O, O> {
  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param proj Projection to use.
   * @param view View to use.
   * @param inner Index to wrap.
   * @param norefine Refinement disable flag.
   */
  public LngLatAsECEFIndex(Relation<O> relation, Projection<O, O> proj, Relation<O> view, Index inner, boolean norefine) {
    super(relation, proj, view, inner, norefine);
  }

  @Override
  public String getLongName() {
    return "projected " + inner.getLongName();
  }

  @Override
  public String getShortName() {
    return "proj-" + inner.getShortName();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof KNNIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    if (!LngLatDistanceFunction.class.isInstance(distanceQuery.getDistanceFunction())) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    SpatialPrimitiveDistanceQuery<O, DoubleDistance> innerQuery = EuclideanDistanceFunction.STATIC.instantiate(view);
    KNNQuery<O, DoubleDistance> innerq = ((KNNIndex<O>) inner).getKNNQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return (KNNQuery<O, D>) new ProjectedKNNQuery<DoubleDistance>((DistanceQuery<O, DoubleDistance>) distanceQuery, innerq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof RangeIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    if (!LngLatDistanceFunction.class.isInstance(distanceQuery.getDistanceFunction())) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    SpatialPrimitiveDistanceQuery<O, DoubleDistance> innerQuery = EuclideanDistanceFunction.STATIC.instantiate(view);
    RangeQuery<O, DoubleDistance> innerq = ((RangeIndex<O>) inner).getRangeQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return (RangeQuery<O, D>) new ProjectedRangeQuery<DoubleDistance>((DistanceQuery<O, DoubleDistance>) distanceQuery, innerq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof RKNNIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    if (!LngLatDistanceFunction.class.isInstance(distanceQuery.getDistanceFunction())) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    SpatialPrimitiveDistanceQuery<O, DoubleDistance> innerQuery = EuclideanDistanceFunction.STATIC.instantiate(view);
    RKNNQuery<O, DoubleDistance> innerq = ((RKNNIndex<O>) inner).getRKNNQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return (RKNNQuery<O, D>) new ProjectedRKNNQuery<DoubleDistance>((DistanceQuery<O, DoubleDistance>) distanceQuery, innerq);
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Data type.
   */
  public static class Factory<O extends NumberVector<?>> extends ProjectedIndex.Factory<O, O> {
    /**
     * Constructor.
     * 
     * @param inner Inner index
     * @param materialize Flag to materialize the projection
     * @param norefine Flag to disable refinement of distances
     */
    public Factory(IndexFactory<O, ?> inner, boolean materialize, boolean norefine) {
      super(new LngLatToECEFProjection<O>(), inner, materialize, norefine);
    }

    @Override
    public ProjectedIndex<O, O> instantiate(Relation<O> relation) {
      if (!proj.getInputDataTypeInformation().isAssignableFromType(relation.getDataTypeInformation())) {
        return null;
      }
      proj.initialize(relation.getDataTypeInformation());
      final Relation<O> view;
      if (materialize) {
        DBIDs ids = relation.getDBIDs();
        WritableDataStore<O> content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, proj.getOutputDataTypeInformation().getRestrictionClass());
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          content.put(iter, proj.project(relation.get(iter)));
        }
        view = new MaterializedRelation<>(relation.getDatabase(), proj.getOutputDataTypeInformation(), ids, "projected data", content);
      } else {
        view = new ProjectedView<>(relation, proj);
      }
      Index inneri = inner.instantiate(view);
      if (inneri == null) {
        return null;
      }
      return new LngLatAsECEFIndex<>(relation, proj, view, inneri, norefine);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @param <O> Outer object type.
     */
    public static class Parameterizer<O extends NumberVector<?>> extends AbstractParameterizer {
      /**
       * Inner index factory.
       */
      IndexFactory<O, ?> inner;

      /**
       * Whether to use a materialized view, or a virtual view.
       */
      boolean materialize = false;

      /**
       * Disable refinement of distances.
       */
      boolean norefine = false;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<IndexFactory<O, ?>> innerP = new ObjectParameter<>(ProjectedIndex.Factory.Parameterizer.INDEX_ID, IndexFactory.class);
        if (config.grab(innerP)) {
          inner = innerP.instantiateClass(config);
        }

        Flag materializeF = new Flag(ProjectedIndex.Factory.Parameterizer.MATERIALIZE_FLAG);
        if (config.grab(materializeF)) {
          materialize = materializeF.isTrue();
        }

        Flag norefineF = new Flag(ProjectedIndex.Factory.Parameterizer.DISABLE_REFINE_FLAG);
        if (config.grab(norefineF)) {
          norefine = norefineF.isTrue();
        }
      }

      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(inner, materialize, norefine);
      }
    }
  }
}