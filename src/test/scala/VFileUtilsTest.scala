import com.intridea.io.vfs.provider.s3.S3FileProvider
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider
import org.apache.commons.vfs2.{FileObject, VFS, FileSystemManager}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import VFileUtils.RichFileObject

@RunWith(classOf[JUnitRunner])
class VFileUtilsTest extends WordSpec {
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
  if (!dir.exists)
    dir.createFolder()
  val file = dir.resolveFile("testFile.txt")
  if (!file.exists)
    file.createFile()

  "RichFileObject" should {
    "isFile or isDirectory" in {
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

    "toFiles" in {
      assert(file.toAbsoluteName=="/testDir/testFile.txt")
    }
  }
}
