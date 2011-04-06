/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.document.mongodb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.document.mongodb.geo.Circle;
import org.springframework.data.document.mongodb.geo.Point;
import org.springframework.data.document.mongodb.query.Criteria;
import org.springframework.data.document.mongodb.query.GeospatialIndex;
import org.springframework.data.document.mongodb.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
/**
 * Modified from https://github.com/deftlabs/mongo-java-geospatial-example
 * @author Mark Pollack
 *
 */
public class GeoSpatialTests {

  private static final Log LOGGER = LogFactory.getLog(GeoSpatialTests.class);
  private final String[] collectionsToDrop = new String[]{"newyork"};

  ApplicationContext applicationContext;
  MongoTemplate template;

  @Before
  public void setUp() throws Exception {
    Mongo mongo = new Mongo();
    DB db = mongo.getDB("geospatial");
    for (String coll : collectionsToDrop) {
      db.getCollection(coll).drop();
    }
    applicationContext = new ClassPathXmlApplicationContext("/geospatial.xml");
    template = applicationContext.getBean(MongoTemplate.class);
    template.setWriteConcern(WriteConcern.FSYNC_SAFE);
    template.ensureIndex(new GeospatialIndex("location"));
    indexCreated();
    addVenues();
  }

  private void addVenues() {
    
    template.insert(new Venue("Penn Station", -73.99408, 40.75057));
    template.insert(new Venue("10gen Office", -73.99171, 40.738868));
    template.insert(new Venue("Flatiron Building", -73.988135, 40.741404));
    template.insert(new Venue("Players Club", -73.997812, 40.739128));
    template.insert(new Venue("City Bakery ", -73.992491, 40.738673));
    template.insert(new Venue("Splash Bar", -73.992491, 40.738673));
    template.insert(new Venue("Momofuku Milk Bar", -73.985839, 40.731698));
    template.insert(new Venue("Shake Shack", -73.98820, 40.74164));
    template.insert(new Venue("Penn Station", -73.99408, 40.75057));
    template.insert(new Venue("Empire State Building", -73.98602, 40.74894));
    //template.insert(new Venue("Washington Square Park", -73.99756, 40.73083));
    template.insert(new Venue("Ulaanbaatar, Mongolia", 106.9154, 47.9245));
    template.insert(new Venue("Maplewood, NJ", -74.2713, 40.73137));
  }

  @Test
  public void withinCircle() {
    
    Circle circle = new Circle(-73.99171, 40.738868, 0.01);
    List<Venue> venues = template.find(new Query(Criteria.where("location").within(circle)), Venue.class);
    assertThat(venues.size(), equalTo(7));
  }
  
  @Test
  public void nearPoint() {
    Point point = new Point(-73.99171, 40.738868);
    List<Venue> venues = template.find(new Query(Criteria.where("location").near(point).maxDistance(0.01)), Venue.class);
    assertThat(venues.size(), equalTo(7));
  }

  @Test
  public void searchAllData() {
    assertThat(template, notNullValue());
    Venue foundVenue = template.findOne(
        new Query(Criteria.where("name").is("Penn Station")), Venue.class);
    assertThat(foundVenue, notNullValue());
    List<Venue> venues = template.getCollection(Venue.class);
    assertThat(venues.size(), equalTo(12));
  }
  
  public void indexCreated() {
    List<DBObject> indexInfo = getIndexInfo();
    LOGGER.debug(indexInfo);
    assertThat(indexInfo.size(), equalTo(2));
    assertThat(indexInfo.get(1).get("name").toString(), equalTo("location_2d"));
    assertThat(indexInfo.get(1).get("ns").toString(),
        equalTo("geospatial.newyork"));
  }

  // TODO move to MongoAdmin
  public List<DBObject> getIndexInfo() {
    return template.execute(new CollectionCallback<List<DBObject>>() {

      public List<DBObject> doInCollection(DBCollection collection)
          throws MongoException, DataAccessException {
        return collection.getIndexInfo();
      }
    });
  }
  

}