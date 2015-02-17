/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.extensions.filters.layer.data.io;

import au.org.emii.geoserver.extensions.filters.layer.data.Filter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.visitor.UniqueVisitor;
import org.opengis.feature.Feature;

import java.io.IOException;
import java.util.*;

import java.util.ArrayList;
import java.util.List;

import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.type.FeatureType;
import java.lang.reflect.Method;
import org.geotools.data.simple.SimpleFeatureSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Type;

public class PossibleValuesReader {

    public Set read(DataStoreInfo dataStoreInfo, LayerInfo layerInfo, String propertyName )
        throws IOException, NoSuchMethodException, SQLException, IllegalAccessException, InvocationTargetException
    {
        JDBCDataStore store = (JDBCDataStore)dataStoreInfo.getDataStore(null);
        Query query = new Query(null, null, new String[] { });
        UniqueVisitor visitor = new UniqueVisitor(propertyName);
        FeatureType schema = store.getFeatureSource(layerInfo.getName()).getSchema();

        Method storeGetAggregateValueMethod = store.getClass().getDeclaredMethod(
			"getAggregateValue",
            org.opengis.feature.FeatureVisitor.class,
            org.opengis.feature.simple.SimpleFeatureType.class,
            org.geotools.data.Query.class,
            java.sql.Connection.class
        );
        storeGetAggregateValueMethod.setAccessible(true);

        Connection conn = store.getDataSource().getConnection();
        try {
            storeGetAggregateValueMethod.invoke(store, visitor, schema, query, conn);
        }
        finally {
            conn.close();
        }

        // ordered by underlying Object type comparator
        return new TreeSet(visitor.getUnique());
    }
}
