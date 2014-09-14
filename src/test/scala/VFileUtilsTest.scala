import com.intridea.io.vfs.provider.s3.S3FileProvider
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider
import org.apache.commons.vfs2.{FileObject, VFS, FileSystemManager}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import VFileUtils.RichFileObject

@RunWith(classOf[JUnitRunner])
class VFileUtilsTest extends WordSpec {
  def createTestFiles(dir: FileObject): FileObject = {
    if (!dir.exists)
      dir.createFolder()
    val file = dir.resolveFile("testFile.txt")
    if (!file.exists)
      file.createFile()
    file
  }

  val bucket = "mslinntest"

  val fsManager: FileSystemManager = {
    import java.io.FileInputStream
    import java.util.Properties
    import com.intridea.io.vfs.provider.s3.S3FileProvider
    import org.apache.commons.vfs2.auth.StaticUserAuthenticator
    import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder

    val config = new Properties()
    config.load(new FileInputStream(System.getProperty("user.home") + "/.aws/config")) // same authentication file used by aws-cli
    val auth = new StaticUserAuthenticator(null, config.getProperty("aws_access_key_id"), config.getProperty("aws_secret_access_key"))
    val opts = S3FileProvider.getDefaultFileSystemOptions
    DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth)
    VFS.getManager
  }

  val s3Utils    = new VFileUtils(new S3FileProvider, fsManager)
  val localUtils = new VFileUtils(new DefaultLocalFileProvider, fsManager)

  val dir: FileObject = try {
    s3Utils.resolveFile(s"s3://$bucket/testDir")
  } catch {
    case npe: NullPointerException =>
      println("Did you provide AWS credentials?")
      sys.exit(-1)
  }

  val file = createTestFiles(dir)

  "RichFileObject" should {
    "asfd" in {
      println(s"aws s3 ls s3://$bucket/testDir")
      val dir1 = VFS.getManager.resolveFile(s"s3://$bucket/testDir")
      //val children1 = dir1.listO

      val dir2 = VFS.getManager.resolveFile(s"s3://$bucket/testDir/")
      //val children2 = dir2.getChildren
    }

    "isFile and isDirectory" in {
      assert(dir.isDirectory)
      assert(file.isFile)
      assert(!file.isDirectory)
      assert(!dir.isFile)
    }

    "listFiles" in {
      val files = dir.listFiles
      assert(files.length==1)
      assert(files(0).getName.getBaseName=="testFile.txt")
    }

    "toAbsoluteName and toCanonicalName" in {
      assert(file.toAbsoluteName=="/testDir/testFile.txt")
      assert(file.toCanonicalName=="/testDir/testFile.txt")
    }

    "copyDirectory" in {
      val destDir = s3Utils.resolveFile(s"s3://$bucket/testDir2")
      VFileUtils.copyDirectory(dir, destDir)
      assert(destDir.isDirectory)
      val files = destDir.listFiles
      assert(files.length==1)
      assert(files(0).toAbsoluteName=="/testDir2/testFile.txt")
      VFileUtils.deleteDirectory(destDir)
      assert(!destDir.exists)
      val filesNFG = destDir.listFiles
    }

    "cleanDirectory" in {
      VFileUtils.cleanDirectory(dir)
      assert(dir.exists)
      assert(dir.listFiles.length==0)
      createTestFiles(dir)
    }

    "deleteQuietly" in {
      VFileUtils.deleteQuietly(dir)
      assert(!dir.exists)
      createTestFiles(dir)
    }

    "forceDelete" in {
      assert(file.exists)
      VFileUtils.forceDelete(file)
      assert(!file.exists)
      intercept[java.io.FileNotFoundException] {
        VFileUtils.forceDelete(file)
      }

      assert(dir.exists)
      VFileUtils.forceDelete(dir)
      assert(!dir.exists)

      intercept[java.io.IOException] {
        VFileUtils.forceDelete(dir)
      }
      createTestFiles(dir)
    }
  }
}
