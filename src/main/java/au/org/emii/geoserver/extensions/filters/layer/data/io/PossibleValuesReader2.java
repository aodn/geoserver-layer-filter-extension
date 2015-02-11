
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

import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.type.FeatureType;
import java.lang.reflect.Method;
import org.geotools.data.simple.SimpleFeatureSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;

public class PossibleValuesReader2 {

    public List<Filter> read(DataStoreInfo dataStoreInfo/*, LayerInfo layerInfo, List<Filter> filters */) throws IOException, NoSuchMethodException, SQLException, IllegalAccessException, InvocationTargetException   {

		JDBCDataStore store = (JDBCDataStore)dataStoreInfo.getDataStore(null);

		Method storeGetAggregateValueMethod = store.getClass().getDeclaredMethod("getAggregateValue",
            org.opengis.feature.FeatureVisitor.class,
            org.opengis.feature.simple.SimpleFeatureType.class,
            org.geotools.data.Query.class,
            java.sql.Connection.class
        );  
        storeGetAggregateValueMethod.setAccessible(true);


        String [] typeNames = store.getTypeNames();
        String typeName = typeNames[0];

        SimpleFeatureSource source = store.getFeatureSource(typeName);

        FeatureType schema = source.getSchema();

        String name = "title";

        Query query = new Query( null, null, new String[] { } );

        UniqueVisitor visitor = new UniqueVisitor( name);

		Connection conn = store.getDataSource().getConnection();

        storeGetAggregateValueMethod.invoke(store, visitor, schema, query, conn );


        //ContentDataStore store = (ContentDataStore)dataStoreInfo.getDataStore(null);
		//Connection conn = store.getDataSource().getConnection(); 
/*
        setFilterValues(dataStoreInfo, layerInfo, getValueFilters(filters), getFeatureSource(layerInfo));
        return filters;
*/
		return null;
    }

    private void setFilterValues(DataStoreInfo dataStoreInfo, LayerInfo layerInfo, List<Filter> filters, FeatureSource featureSource) throws IOException {
        ContentDataStore dataStore = (ContentDataStore)dataStoreInfo.getDataStore(null);
        Query query = new Query(layerInfo.getName(), org.opengis.filter.Filter.INCLUDE, getFilterNames(filters));

        // Untried and may need some tweaking to get sorting occurring at the db level rather than relying on a TreeSet
        //query.setSortBy(new SortBy[] { SortBy.NATURAL_ORDER });

        for (Filter filter : filters) {
            UniqueVisitor visitor = new UniqueVisitor(filter.getName());
            ContentFeatureSource contentFeatureSource = dataStore.getFeatureSource((String)dataStoreInfo.getConnectionParameters().get("schema"));
            contentFeatureSource.accepts(query, visitor, null);
            System.out.println(contentFeatureSource.getFeatures());

            // Need to do some sort of transformation to get a FeatureCollection to a Set
            //filter.setValues(new TreeSet<String>(contentFeatureSource.getFeatures()));
        }
    }

    private Map<String, Set> initFilterValues(List<Filter> filters) {
        Map<String, Set> filterValues = new LinkedHashMap<String, Set>(filters.size());
        for (Filter filter : filters) {
            filter.setValues(new TreeSet<String>());
            filterValues.put(filter.getName(), filter.getValues());
        }

        return filterValues;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    private boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    private String getFeaturePropertyValue(Feature feature, String propertyName) {
        Object value = feature.getProperty(propertyName).getValue();
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    private FeatureSource getFeatureSource(LayerInfo layerInfo) throws IOException {
        return ((FeatureTypeInfo)layerInfo.getResource()).getFeatureSource(null, null);
    }

    private List<Filter> getValueFilters(List<Filter> filters) {
        List<Filter> possibleValueFilters = new ArrayList<Filter>(filters);
        CollectionUtils.filter(possibleValueFilters, getPredicate());
        return possibleValueFilters;
    }

    private Predicate getPredicate() {
        return new Predicate() {
            public boolean evaluate(Object o) {
                return "string".equals(((Filter)o).getType().toLowerCase());
            }
        };
    }

    private String[] getFilterNames(List<Filter> filters) {
        String[] filterNames = new String[filters.size()];

        int i = 0;
        for (Filter filter : filters) {
            filterNames[i++] = filter.getName();
        }

        return filterNames;
    }
}
