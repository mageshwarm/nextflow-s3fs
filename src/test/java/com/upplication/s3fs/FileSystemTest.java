/*
 * Copyright 2020, Seqera Labs
 * Copyright 2013-2019, Centre for Genomic Regulation (CRG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Javier Arnáiz @arnaix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.upplication.s3fs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.google.common.collect.ImmutableMap;
import com.upplication.s3fs.util.AmazonS3ClientMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class FileSystemTest {
	
	private FileSystem fs;
	private FileSystem fsMem;
	private S3FileSystemProvider provider;
	
	@Before
	public void cleanup() throws IOException{
		fsMem = MemoryFileSystemBuilder.newLinux().build("basescheme");
		// close old
		try{
			FileSystems.getFileSystem(URI.create("s3:///")).close();
		}
		catch(FileSystemNotFoundException e){}
		// create a new
		provider = spy(new S3FileSystemProvider());
		doReturn(new Properties()).when(provider).loadAmazonProperties();
		// by default with two buckets
		Path bucketA = Files.createDirectories(fsMem.getPath("/base", "bucketA"));
		Path bucketB = Files.createDirectories(fsMem.getPath("/base", "bucketB"));
		Files.createFile(bucketA.resolve("file1"));
		Files.createFile(bucketB.resolve("file2"));
		mockFileSystem(fsMem.getPath("/base"));
		//
		fs = provider.newFileSystem(URI.create("s3:///"), buildFakeEnv());
	}
	
	@After
	public void closeMemory() throws IOException{
		fsMem.close();
	}
	
	
	private void mockFileSystem(final Path memoryBucket){
		try {
			AmazonS3ClientMock clientMock = new AmazonS3ClientMock(memoryBucket);
			S3FileSystem s3ileS3FileSystem = new S3FileSystem(provider, clientMock, "endpoint");
			doReturn(s3ileS3FileSystem).when(provider).createFileSystem(any(URI.class), anyObject(), anyObject());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    @Test
    public void getPathFirst() throws IOException {
        assertEquals(fs.getPath("/bucket"),
                fs.getPath("/bucket"));

        assertEquals(fs.getPath("file"),
                fs.getPath("file"));
    }

    @Test
    public void getPathFirstWithMultiplesPaths() throws IOException {
        assertEquals(fs.getPath("/bucket/path/to/file"),
            fs.getPath("/bucket/path/to/file"));
        assertNotEquals(fs.getPath("/bucket/path/other/file"),
                fs.getPath("/bucket/path/to/file"));

        assertEquals(fs.getPath("dir/path/to/file"),
                fs.getPath("dir/path/to/file"));
        assertNotEquals(fs.getPath("dir/path/other/file"),
                fs.getPath("dir/path/to/file"));
    }

    @Test
    public void getPathFirstAndMore() throws IOException {
        Path actualAbsolute = fs.getPath("/bucket", "dir", "file");
        assertEquals(fs.getPath("/bucket", "dir", "file"), actualAbsolute);
        assertEquals(fs.getPath("/bucket/dir/file"), actualAbsolute);

        Path actualRelative = fs.getPath("dir", "dir", "file");
        assertEquals(fs.getPath("dir", "dir", "file"), actualRelative);
        assertEquals(fs.getPath("dir/dir/file"), actualRelative);
    }

    @Test
    public void getPathFirstAndMoreWithMultiplesPaths() throws IOException {
        Path actual = fs.getPath("/bucket", "dir/file");
        assertEquals(fs.getPath("/bucket", "dir/file"), actual);
        assertEquals(fs.getPath("/bucket/dir/file"), actual);
        assertEquals(fs.getPath("/bucket", "dir", "file"), actual);
    }

    @Test
    public void getPathFirstWithMultiplesPathsAndMoreWithMultiplesPaths() throws IOException {
        Path actual = fs.getPath("/bucket/dir", "dir/file");
        assertEquals(fs.getPath("/bucket/dir", "dir/file"), actual);
        assertEquals(fs.getPath("/bucket/dir/dir/file"), actual);
        assertEquals(fs.getPath("/bucket", "dir", "dir", "file"), actual);
        assertEquals(fs.getPath("/bucket/dir/dir", "file"), actual);
    }

    @Test
    public void getPathRelativeAndAbsoulte() throws IOException {
        assertNotEquals(fs.getPath("/bucket"), fs.getPath("bucket"));
        assertNotEquals(fs.getPath("/bucket/dir"), fs.getPath("bucket/dir"));
        assertNotEquals(fs.getPath("/bucket", "dir"), fs.getPath("bucket", "dir"));
        assertNotEquals(fs.getPath("/bucket/dir", "dir"), fs.getPath("bucket/dir", "dir"));
        assertNotEquals(fs.getPath("/bucket", "dir/file"), fs.getPath("bucket", "dir/file"));
        assertNotEquals(fs.getPath("/bucket/dir", "dir/file"), fs.getPath("bucket/dir", "dir/file"));
    }

    @Test
    public void duplicatedSlashesAreDeleted() throws IOException {
        Path actualFirst = fs.getPath("/bucket//file");
        assertEquals(fs.getPath("/bucket/file"), actualFirst);
        assertEquals(fs.getPath("/bucket", "file"), actualFirst);

        Path actualFirstAndMore = fs.getPath("/bucket//dir", "dir//file");
        assertEquals(fs.getPath("/bucket/dir/dir/file"), actualFirstAndMore);
        assertEquals(fs.getPath("/bucket", "dir/dir/file"), actualFirstAndMore);
        assertEquals(fs.getPath("/bucket/dir", "dir/file"), actualFirstAndMore);
        assertEquals(fs.getPath("/bucket/dir/dir", "file"), actualFirstAndMore);
    }


    @Test
	public void readOnlyAlwaysFalse(){
		assertTrue(!fs.isReadOnly());
	}
	
	@Test
	public void getSeparatorSlash(){
		assertEquals("/", fs.getSeparator());
		assertEquals("/", S3Path.PATH_SEPARATOR);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void getPathMatcherThrowException(){
		fs.getPathMatcher("");
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void getUserPrincipalLookupServiceThrowException(){
		fs.getUserPrincipalLookupService();
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void newWatchServiceThrowException() throws Exception {
		fs.newWatchService();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void getPathWithoutBucket() {
		fs.getPath("//path/to/file");
	}
	
	@Test
	public void getFileStoresReturnEmptyList(){
		Iterable<FileStore> result = fs.getFileStores();
		
		assertNotNull(result);
		assertNotNull(result.iterator());
		assertTrue(!result.iterator().hasNext());
	}
	
	@Test
	public void getRootDirectoriesReturnBuckets() {
		
		Iterable<Path> paths = fs.getRootDirectories();
		
		assertNotNull(paths);
		
		int size = 0;
		boolean bucketNameA = false;
		boolean bucketNameB = false;
		
		for (Path path : paths) {
			String name = path.getFileName().toString();
			if (name.equals("bucketA")) {
				bucketNameA = true;
			}
			else if (name.equals("bucketB")) {
				bucketNameB = true;
			}
			size++;
		}
		
		assertEquals(2, size);
		assertTrue(bucketNameA);
		assertTrue(bucketNameB);
	}
	
	@Test
	public void supportedFileAttributeViewsReturnBasic(){
		Set<String> operations = fs.supportedFileAttributeViews();
		
		assertNotNull(operations);
		assertTrue(!operations.isEmpty());
		
		for (String operation: operations){
			assertEquals("basic", operation);
		}
	}
	
	@Test
	public void getRootDirectories(){
		fs.getRootDirectories();
	}
	
	@Test
	public void close() throws IOException {
		assertTrue(fs.isOpen());
		fs.close();
		assertTrue(!fs.isOpen());
	}
	
	private Map<String, ?> buildFakeEnv(){
		return ImmutableMap.<String, Object> builder()
				.put(S3FileSystemProvider.ACCESS_KEY, "access key")
				.put(S3FileSystemProvider.SECRET_KEY, "secret key").build();
	}

    private static void assertNotEquals(Object a, Object b){
        assertTrue(a + " are not equal to: " + b, !a.equals(b));
    }
}
