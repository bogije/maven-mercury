package org.apache.maven.mercury.ant.tasks;

import java.io.File;

import org.apache.maven.mercury.spi.http.server.SimpleTestServer;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Path;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class MecuryAntTest
extends BuildFileTest 
{
  static final String _localRepoDir = "./target/repo";
  
  static final String _writeRepoDir = "./target/test-repo";
  static       File   _writeRepoDirFile;
  
  static final String _verifyRepoDir = "./target/test-verify-repo";
  static       File   _verifyRepoDirFile;
  
  static final String _compileDir = "./target/compile-classes";
  static       File   _compileDirFile;
  
  static final String _jarDir = "./target/compile-target";
  static       File   _jarDirFile;

  static final String _remoteRepoDir = "./target/test-classes/remoteRepo";
  static final String _remoteRepoUrlPrefix = "http://localhost:";
  static final String _remoteRepoUrlSufix = "/repo";

  static final String _pathId = "class-path";
  
  SimpleTestServer _jetty;
  String _port;
    
  Resolver _resolver;
  Config   _config;
  Dep      _dep;
  
  Dep.Dependency _asm;
  Dep.Dependency _ant;
    
  //-----------------------------------
  final class Resolver
  extends ResolveTask
  {
    @SuppressWarnings("deprecation")
    public Resolver()
    {
      project = new Project();
      project.init();
      target = new Target();
    }
  }
  //-----------------------------------
  final class Writer
  extends WriteTask
  {
    @SuppressWarnings("deprecation")
    public Writer()
    {
      project = new Project();
      project.init();
      target = new Target();
    }
  }
  //-----------------------------------
  public MecuryAntTest( String name )
  {
    super( name );
  }
  //-----------------------------------
  @Override
  protected void setUp()
  throws Exception
  {
    _dep = new Dep();
    _dep.setId( "my-lib" );
    
    _asm = _dep.createDependency();
    _asm.setName( "asm:asm-xml:3.0" );
    
    _ant = _dep.createDependency();
    _ant.setName( "ant:ant:1.6.5" );
    
    _config = new Config();
    _config.setId( "conf" );
    
    File lrDir = new File( _localRepoDir );
    FileUtil.delete( lrDir );
    lrDir.mkdirs();
    
    Config.Repo localRepo = _config.createRepo();
    localRepo.setId( "localRepo" );
    localRepo.setDir( _localRepoDir );
    
    File rrDir = new File( _remoteRepoDir );
    _jetty = new SimpleTestServer( rrDir, _remoteRepoUrlSufix );
    _jetty.start();
    _port = ""+_jetty.getPort();
    
    Config.Repo remoteRepo = _config.createRepo();
    remoteRepo.setId( "remoteRepo" );
    remoteRepo.setUrl( _remoteRepoUrlPrefix + _port + _remoteRepoUrlSufix );
        
    _resolver = new Resolver();
    _resolver.setDepid( _dep.getId() );
    _resolver.setConfigid( _config.getId() );
    
    _resolver.setPathid( _pathId );
    
    Project project = _resolver.getProject();
    
    project.addReference( _config.getId(), _config );
    project.addReference( _dep.getId(), _dep );

    System.setProperty( "ant.home", ".src/test/apache-ant-1.6.5" );
    
    configureProject("build.xml");

    _writeRepoDirFile = new File( _writeRepoDir );
    FileUtil.delete( _writeRepoDirFile );
    _writeRepoDirFile.mkdirs();

    _verifyRepoDirFile = new File( _verifyRepoDir );
    FileUtil.delete( _verifyRepoDirFile );
    _verifyRepoDirFile.mkdirs();
    
    _compileDirFile   = new File( _compileDir );
    FileUtil.delete( _compileDirFile );
    _compileDirFile.mkdirs();
    
    _jarDirFile       = new File( _jarDir );
    FileUtil.delete( _jarDirFile );
    _jarDirFile.mkdirs();
  }
  //-----------------------------------
  @Override
  protected void tearDown()
  throws Exception
  {
    _jetty.stop();
    _jetty.destroy();
    
    System.out.println("Jetty on :"+_port+" destroyed\n<========\n\n");
  }
  //-----------------------------------
  public void testReadDependencies()
  {
    String title = "resolver";
    System.out.println("========> start "+title);
    System.out.flush();
    
    _resolver.execute();
    
    Project pr = _resolver.getProject();
    
    Path path = (Path)pr.getReference( _pathId );
    
    assertNotNull( path );
    
    String [] list = path.list();
    
    assertNotNull( list );

    assertEquals( 6, list.length );
    
//    System.out.println("\n==== Files found ====");
//    for( String s : list )
//      System.out.println(s);
    
    File af = new File( _localRepoDir, "/ant/ant/1.6.5/ant-1.6.5.jar" );
    assertTrue( af.exists() );
    
    af = new File( _localRepoDir, "/asm/asm-util/3.0/asm-util-3.0.jar" );
    assertTrue( af.exists() );
    
    af = new File( _localRepoDir, "/asm/asm-xml/3.0/asm-xml-3.0.jar" );
    assertTrue( af.exists() );
    
    af = new File( _localRepoDir, "/asm/asm-tree/3.0/asm-tree-3.0.jar" );
    assertTrue( af.exists() );
    
    af = new File( _localRepoDir, "/asm/asm/3.0/asm-3.0.jar" );
    assertTrue( af.exists() );
    
  }
  //-----------------------------------
  public void testCompile()
  {
    String title = "compile";
    System.out.println("========> start "+title);
    System.out.flush();
    
    File af = new File( _compileDirFile, "T.class" );
 
    assertFalse( af.exists() );

    File jar = new File( _jarDirFile, "t.jar" );
    
    assertFalse( jar.exists() );
    
    executeTarget("compile");
    
    assertTrue( af.exists() );

    assertTrue( jar.exists() );
  }
  //-----------------------------------
  public void testWriteToRepository()
  {
    String title = "write";
    System.out.println("========> start "+title);
    System.out.flush();
    
    File af = new File( _writeRepoDirFile, "/t/t/1.0/t-1.0.jar" );
    assertFalse( af.exists() );

    File ap = new File( _writeRepoDirFile, "/t/t/1.0/t-1.0.pom" );
    assertFalse( ap.exists() );
    
    executeTarget("deploy");
    
    assertTrue( af.exists() );
    assertTrue( ap.exists() );
  }
  //-----------------------------------
  public void testVerifyPgp()
  {
    String title = "write-verify-pgp";
    System.out.println("========> start "+title);
    System.out.flush();
    
    File af = new File( _verifyRepoDirFile, "/t/t/1.0/t-1.0.jar" );
    assertFalse( af.exists() );
    
    File sig = new File( _verifyRepoDirFile, "/t/t/1.0/t-1.0.jar.asc" );
    assertFalse( sig.exists() );
    
    executeTarget("deploy-verify");
    
    assertTrue( af.exists() );
    assertTrue( sig.exists() );
  }
  //-----------------------------------
}