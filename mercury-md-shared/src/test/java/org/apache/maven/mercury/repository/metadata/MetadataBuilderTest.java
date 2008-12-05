/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.maven.mercury.repository.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.TimeUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class MetadataBuilderTest
extends TestCase
{
  MetadataBuilder mb;
  File testBase = new File("./target/test-classes/controlledRepo");

  //-------------------------------------------------------------------------
  @Override
  protected void setUp()
  throws Exception
  {
    File temp = new File( testBase, "group-maven-metadata-write.xml");
    if( temp.exists() )
      temp.delete();
  }
  //-------------------------------------------------------------------------
  protected void tearDown()
  throws Exception
  {
  }
  //-------------------------------------------------------------------------
  public void testReadGroupMd()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
     Metadata mmd = MetadataBuilder.read(  new FileInputStream( groupMd ) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 4, versions.size() );
  }
  //-------------------------------------------------------------------------
  public void testWriteGroupMd()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata-write.xml");
    Metadata md = new Metadata();
    md.setGroupId( "a" );
    md.setArtifactId( "a" );
    md.setVersion( "1.0.0" );
    Versioning v = new Versioning();
    v.addVersion( "1.0.0" );
    v.addVersion( "2.0.0" );
    md.setVersioning( v );
    
     MetadataBuilder.write(  md, new FileOutputStream( groupMd ) );
     Metadata mmd = MetadataBuilder.read( new FileInputStream(groupMd) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("1.0.0", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 2, versions.size() );
  }
  //-------------------------------------------------------------------------
  public void testAddPluginOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );

    Plugin plugin = new Plugin();
    plugin.setArtifactId( "some-artifact-id" );
    plugin.setName( "Some Plugin" );
    plugin.setPrefix( "some" );

    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, new AddPluginOperation( new PluginOperand(plugin) ) );

    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );

     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals(1, mmd.getPlugins().size() );
     assertEquals("some-artifact-id", ((Plugin)mmd.getPlugins().get( 0 )).getArtifactId() );
     assertEquals("Some Plugin", ((Plugin)mmd.getPlugins().get( 0 )).getName() );
     assertEquals("some", ((Plugin)mmd.getPlugins().get( 0 )).getPrefix() );

     // now let's drop plugin
     targetBytes = FileUtil.readRawData( resFile );
     resBytes = MetadataBuilder.changeMetadata( targetBytes, new RemovePluginOperation( new PluginOperand(plugin) ) );

     Metadata mmd2 = MetadataBuilder.read( new ByteArrayInputStream(resBytes) );

     assertNotNull( mmd2 );
     assertEquals(0, mmd2.getPlugins().size() );
  }
  //-------------------------------------------------------------------------
  public void testMergeOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );

    Metadata source = new Metadata();
    source.setGroupId( "a" );
    source.setArtifactId( "a" );
    source.setVersion( "1.0.0" );
    Versioning v = new Versioning();
    v.addVersion( "1.0.0" );
    v.addVersion( "2.0.0" );
    source.setVersioning( v );
    
    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, new MergeOperation( new MetadataOperand(source) ) );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 6, versions.size() );
     assertTrue( versions.contains("1") );
     assertTrue( versions.contains("2") );
     assertTrue( versions.contains("3") );
     assertTrue( versions.contains("4") );
     assertTrue( versions.contains("1.0.0") );
     assertTrue( versions.contains("2.0.0") );
  }
  //-------------------------------------------------------------------------
  public void testAddVersionOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );

    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, new AddVersionOperation( new StringOperand("5") ) );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 5, versions.size() );
     assertTrue( versions.contains("1") );
     assertTrue( versions.contains("2") );
     assertTrue( versions.contains("3") );
     assertTrue( versions.contains("4") );
     assertTrue( versions.contains("5") );
  }
  //-------------------------------------------------------------------------
  public void testAddVersionTwiceOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );
    
    Metadata checkMd = MetadataBuilder.getMetadata( targetBytes ); 

    assertNotNull( checkMd );
    assertEquals("a", checkMd.getGroupId() );
    assertEquals("a", checkMd.getArtifactId() );
    assertEquals("4", checkMd.getVersion() );

    assertNotNull( checkMd.getVersioning() );
    
    List<String> checkVersions = checkMd.getVersioning().getVersions();
   
    assertNotNull( checkVersions );
    assertEquals( 4, checkVersions.size() );
    assertTrue( checkVersions.contains("1") );
    assertTrue( checkVersions.contains("2") );
    assertTrue( checkVersions.contains("3") );
    assertTrue( checkVersions.contains("4") );
    assertFalse( checkVersions.contains("5") );
    
    List<MetadataOperation> ops = new ArrayList<MetadataOperation>();
    ops.add( new AddVersionOperation( new StringOperand("5") ) );
    ops.add( new AddVersionOperation( new StringOperand("5") ) );

    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, ops );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 5, versions.size() );
     assertTrue( versions.contains("1") );
     assertTrue( versions.contains("2") );
     assertTrue( versions.contains("3") );
     assertTrue( versions.contains("4") );
     assertTrue( versions.contains("5") );
  }
  //-------------------------------------------------------------------------
  public void testRemoveVersionOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );

    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, new RemoveVersionOperation( new StringOperand("1") ) );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 3, versions.size() );
     assertTrue( !versions.contains("1") );
     assertTrue( versions.contains("2") );
     assertTrue( versions.contains("3") );
     assertTrue( versions.contains("4") );
  }
  //-------------------------------------------------------------------------
  public void testSetSnapshotOperation()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );
    
    Snapshot sn = new Snapshot();
    sn.setLocalCopy( false );
    sn.setBuildNumber( 35 );
    String ts = TimeUtil.getUTCTimestamp();
    sn.setTimestamp( ts );

    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, new SetSnapshotOperation( new SnapshotOperand(sn) ) );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
     Snapshot snapshot = mmd.getVersioning().getSnapshot();
     assertNotNull( snapshot );
     assertEquals( ts, snapshot.getTimestamp() );
     
     // now let's drop sn
     targetBytes = FileUtil.readRawData( resFile );
     resBytes = MetadataBuilder.changeMetadata( targetBytes, new SetSnapshotOperation( new SnapshotOperand(null) ) );
     
     Metadata mmd2 = MetadataBuilder.read( new ByteArrayInputStream(resBytes) );

     assertNotNull( mmd2 );
     assertEquals("a", mmd2.getGroupId() );
     assertEquals("a", mmd2.getArtifactId() );
     assertEquals("4", mmd2.getVersion() );

     assertNotNull( mmd2.getVersioning() );
     
     snapshot = mmd2.getVersioning().getSnapshot();
     assertNull( snapshot );
  }
  //-------------------------------------------------------------------------
  public void testMultipleOperations()
  throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
  {
    File groupMd = new File( testBase, "group-maven-metadata.xml");
    byte [] targetBytes = FileUtil.readRawData( groupMd );

    ArrayList<MetadataOperation> ops = new ArrayList<MetadataOperation>(2);
    ops.add( new RemoveVersionOperation( new StringOperand("1") ) );
    ops.add( new AddVersionOperation( new StringOperand("8") ) );
    
    byte [] resBytes = MetadataBuilder.changeMetadata( targetBytes, ops  );
    
    File resFile = new File( testBase, "group-maven-metadata-write.xml");

    FileUtil.writeRawData( resFile, resBytes );
    
     Metadata mmd = MetadataBuilder.read( new FileInputStream(resFile) );

     assertNotNull( mmd );
     assertEquals("a", mmd.getGroupId() );
     assertEquals("a", mmd.getArtifactId() );
     assertEquals("4", mmd.getVersion() );

     assertNotNull( mmd.getVersioning() );
    
     List<String> versions = mmd.getVersioning().getVersions();
    
     assertNotNull( versions );
     assertEquals( 4, versions.size() );
     assertTrue( !versions.contains("1") );
     assertTrue( versions.contains("2") );
     assertTrue( versions.contains("3") );
     assertTrue( versions.contains("4") );
     assertTrue( versions.contains("8") );
  }
  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
}
