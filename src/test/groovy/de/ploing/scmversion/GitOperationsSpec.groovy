package de.ploing.scmversion

import de.ploing.scmversion.git.GitOperations
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * @author Stefan Schlott
 */
class GitOperationsSpec extends Specification {
    static File testRepoDir

    def extractZip(InputStream input, File destDir) {
        byte[] buffer = new byte[1024]
        ZipInputStream zis = new ZipInputStream(input);
        ZipEntry ze = zis.getNextEntry();
        while(ze!=null) {
            String fileName = ze.getName()
            File newFile = new File(destDir, fileName)
            if (ze.isDirectory()) {
                newFile.mkdirs()
            } else {
                new File(newFile.getParent()).mkdirs()
                FileOutputStream fos = new FileOutputStream(newFile)
                int len
                while ((len=zis.read(buffer))>0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
            }
            zis.closeEntry()
            ze = zis.getNextEntry()
        }
        //close last ZipEntry
        zis.closeEntry()
        zis.close()
        input.close()
    }

    def setupSpec() {
        testRepoDir = new File('./build/testrepos').absoluteFile
        if (!testRepoDir.exists()) {
            testRepoDir.mkdirs()
        }
        ['emptyrepo', 'linearrepo', 'mergedrepo'].each { name ->
            extractZip(getClass().classLoader.getResourceAsStream("${name}.zip"), testRepoDir)
        }
    }

    def "GitOperations don't work on empty repo"() {
        setup:
        when:
            new GitOperations(new File(testRepoDir, 'emptyrepo'))
        then:
            thrown(IllegalArgumentException)
    }

    def "Test repo is clean"() {
        setup:
            SCMOperations ops = new GitOperations(new File(testRepoDir, 'linearrepo'))
        when:
            def isDirty = ops.isRepoDirty()
        then:
            isDirty==false
    }

    def "Creating a file renders repo dirty"() {
        setup:
            SCMOperations ops = new GitOperations(new File(testRepoDir, 'linearrepo'))
            File tmpFile = new File(testRepoDir, 'linearrepo/test.tmp')
        when:
            tmpFile.createNewFile()
            def isDirty = ops.isRepoDirty()
            tmpFile.delete()
        then:
            isDirty==true
    }

    def "Tags gives all tags in linear history"() {
        setup:
            SCMOperations ops = new GitOperations(new File(testRepoDir, 'linearrepo'))
        when:
            def head = ops.headVersion
            def tags = ops.tags
        then:
            head!=null
            tags.size()==4
    }

    def "HeadTags gives tags of head in linear history"() {
        setup:
            SCMOperations ops = new GitOperations(new File(testRepoDir, 'linearrepo'))
        when:
            def head = ops.headVersion
            def tags = ops.headTags
        then:
            head!=null
            tags.size()==1
    }

    def "Tags gives all tags in branched history"() {
        setup:
            SCMOperations ops = new GitOperations(new File(testRepoDir, 'mergedrepo'))
        when:
            def head = ops.headVersion
            def tags = ops.tags
        then:
            head!=null
            tags.size()==4
    }
}
