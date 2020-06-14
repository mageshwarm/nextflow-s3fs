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
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class S3PathTest {
	
	@Before
	public void setup() throws IOException {
        try {
            FileSystems.newFileSystem(URI.create("s3:///"), ImmutableMap.<String, Object>of());
        }
        catch (FileSystemAlreadyExistsException e) {
            // already exist. Only when all test are executed in parallel
        }
	}

    @After
    public void close() throws IOException {
        try {
            FileSystems.getFileSystem(URI.create("s3:///")).close();
        }
        catch (FileSystemNotFoundException e) {
            // already closed
        }
    }

	
    @Test
    public void createNoPath() {
        S3Path path = forPath("/bucket");

        assertEquals("bucket", path.getBucket());
        assertEquals("", path.getKey());
    }

    @Test
    public void createWithTrailingSlash() {
        S3Path path = forPath("/bucket/");

        assertEquals(path.getBucket(), "bucket");
        assertEquals(path.getKey(), "");
    }

    @Test
    public void createWithPath() {
        S3Path path = forPath("/bucket/path/to/file");

        assertEquals(path.getBucket(), "bucket");
        assertEquals(path.getKey(), "path/to/file");
    }

    @Test
    public void createWithPathAndTrailingSlash() {
        S3Path path = forPath("/bucket/path/to/file/");

        assertEquals("bucket", path.getBucket());
        assertEquals("path/to/file", path.getKey());
    }

    @Test
    public void createRelative() {
        S3Path path = forPath("path/to/file");
        
        assertNull(path.getBucket());
        assertEquals(path.getKey(), "path/to/file");
        assertFalse(path.isAbsolute());
    }

    @Test
    public void getParent() {
        assertEquals(forPath("/bucket/path/to/"), forPath("/bucket/path/to/file").getParent());
        assertEquals(forPath("/bucket/path/to/"), forPath("/bucket/path/to/file/").getParent());
        assertNull(forPath("/bucket/").getParent());
        assertNull(forPath("/bucket").getParent());
    }
    
    @Test
    public void nameCount() {
        assertEquals(forPath("/bucket/path/to/file").getNameCount(), 3);
        assertEquals(forPath("/bucket/").getNameCount(), 0);
    }
    
    @Test
    public void resolve() {
        assertEquals(forPath("/bucket/path/to/dir/").resolve(forPath("child/xyz")), forPath("/bucket/path/to/dir/child/xyz"));
        assertEquals(forPath("/bucket/path/to/dir").resolve(forPath("child/xyz")), forPath("/bucket/path/to/dir/child/xyz"));
        assertEquals(forPath("/bucket/path/to/file").resolve(forPath("")), forPath("/bucket/path/to/file"));  // TODO: should this be "path/to/dir/"
        assertEquals(forPath("path/to/file").resolve(forPath("child/xyz")), forPath("path/to/file/child/xyz"));
        assertEquals(forPath("path/to/file").resolve(forPath("")), forPath("path/to/file")); // TODO: should this be "path/to/dir/"
        assertEquals(forPath("/bucket/path/to/file").resolve(forPath("/bucket2/other/child")), forPath("/bucket2/other/child"));
    }
    
    @Test
    public void name() {
        assertEquals(forPath("/bucket/path/to/file").getName(0), forPath("path/"));
        assertEquals(forPath("/bucket/path/to/file").getName(1), forPath("to/"));
        assertEquals(forPath("/bucket/path/to/file").getName(2), forPath("file"));
    }

    @Test
    public void subPath() {
        assertEquals(forPath("/bucket/path/to/file").subpath(0, 1), forPath("path/"));
        assertEquals(forPath("/bucket/path/to/file").subpath(0, 2), forPath("path/to/"));
        assertEquals(forPath("/bucket/path/to/file").subpath(0, 3), forPath("path/to/file"));
        assertEquals(forPath("/bucket/path/to/file").subpath(1, 2), forPath("to/"));
        assertEquals(forPath("/bucket/path/to/file").subpath(1, 3), forPath("to/file"));
        assertEquals(forPath("/bucket/path/to/file").subpath(2, 3), forPath("file"));
    }
    
    @Test
    public void iterator() {
        Iterator<Path> iterator = forPath("/bucket/path/to/file").iterator();
        
        assertEquals(iterator.next(), forPath("path/"));
        assertEquals(iterator.next(), forPath("to/"));
        assertEquals(iterator.next(), forPath("file"));
    }
    
    @Test
    public void resolveSibling() {
        // absolute (non-root) vs...
        assertEquals(forPath("/bucket/path/to/file").resolveSibling(forPath("other/child")), forPath("/bucket/path/to/other/child"));
        assertEquals(forPath("/bucket/path/to/file").resolveSibling(forPath("/bucket2/other/child")), forPath("/bucket2/other/child"));
        assertEquals(forPath("/bucket/path/to/file").resolveSibling(forPath("")), forPath("/bucket/path/to/"));

        // absolute (root) vs ...
        assertEquals(forPath("/bucket").resolveSibling(forPath("other/child")), forPath("other/child"));
        assertEquals(forPath("/bucket").resolveSibling(forPath("/bucket2/other/child")), forPath("/bucket2/other/child"));
        assertEquals(forPath("/bucket").resolveSibling(forPath("")), forPath(""));

        // relative (empty) vs ...
        assertEquals(forPath("").resolveSibling(forPath("other/child")), forPath("other/child"));
        assertEquals(forPath("").resolveSibling(forPath("/bucket2/other/child")), forPath("/bucket2/other/child"));
        assertEquals(forPath("").resolveSibling(forPath("")), forPath(""));

        // relative (non-empty) vs ...
        assertEquals(forPath("path/to/file").resolveSibling(forPath("other/child")), forPath("path/to/other/child"));
        assertEquals(forPath("path/to/file").resolveSibling(forPath("/bucket2/other/child")), forPath("/bucket2/other/child"));
        assertEquals(forPath("path/to/file").resolveSibling(forPath("")), forPath("path/to/"));
    }
    
    @Test
    public void resolveSiblingString() {
        // absolute (non-root) vs...
        assertEquals(forPath("/bucket/path/to/file").resolveSibling("other/child"), forPath("/bucket/path/to/other/child"));
        assertEquals(forPath("/bucket/path/to/file").resolveSibling("/bucket2/other/child"), forPath("/bucket2/other/child"));
        assertEquals(forPath("/bucket/path/to/file").resolveSibling(""), forPath("/bucket/path/to/"));

        // absolute (root) vs ...
        assertEquals(forPath("/bucket").resolveSibling("other/child"), forPath("other/child"));
        assertEquals(forPath("/bucket").resolveSibling("/bucket2/other/child"), forPath("/bucket2/other/child"));
        assertEquals(forPath("/bucket").resolveSibling(""), forPath(""));

        // relative (empty) vs ...
        assertEquals(forPath("").resolveSibling("other/child"), forPath("other/child"));
        assertEquals(forPath("").resolveSibling("/bucket2/other/child"), forPath("/bucket2/other/child"));
        assertEquals(forPath("").resolveSibling(""), forPath(""));

        // relative (non-empty) vs ...
        assertEquals(forPath("path/to/file").resolveSibling("other/child"), forPath("path/to/other/child"));
        assertEquals(forPath("path/to/file").resolveSibling("/bucket2/other/child"), forPath("/bucket2/other/child"));
        assertEquals(forPath("path/to/file").resolveSibling(""), forPath("path/to/"));
    }
    
    @Test
    public void relativize(){
    	Path path = forPath("/bucket/path/to/file");
    	Path other = forPath("/bucket/path/to/file/hello");
    	
        assertEquals(forPath("hello"), path.relativize(other));
    	
    	// another

    	assertEquals(forPath("file/hello"), forPath("/bucket/path/to/").relativize(forPath("/bucket/path/to/file/hello")));
    	
    	// empty
    	
    	assertEquals(forPath(""), forPath("/bucket/path/to/").relativize(forPath("/bucket/path/to/")));
    }

    // to uri
    
    @Test
    public void toUri() {
    	Path path = forPath("/bucket/path/to/file");
    	URI uri = path.toUri();
    	
    	// the scheme is s3
    	assertEquals("s3", uri.getScheme());
    	
    	// could get the correct fileSystem
    	FileSystem fs = FileSystems.getFileSystem(uri);
    	assertTrue(fs instanceof S3FileSystem);
    	// the host is the endpoint specified in fileSystem
    	assertEquals(((S3FileSystem) fs).getEndpoint(), uri.getHost());
    	
    	// bucket name as first path
    	Path pathActual = fs.provider().getPath(uri);
    	
    	assertEquals(path, pathActual);
    }

    @Test
    public void toUriWithEndpoint() throws IOException {
        FileSystems.getFileSystem(URI.create("s3:///")).close();

        try(FileSystem fs = FileSystems.newFileSystem(URI.create("s3://endpoint/"), ImmutableMap.<String, Object>of())){
            Path path = fs.getPath("/bucket/path/to/file");
            URI uri = path.toUri();
            // the scheme is s3
            assertEquals("s3", uri.getScheme());
            assertEquals("endpoint", uri.getHost());
            assertEquals("/bucket/path/to/file", uri.getPath());
        }
    }
    
    // tests startsWith
    
    @Test
	public void startsWith(){
		assertTrue(forPath("/bucket/file1").startsWith(forPath("/bucket")));
	}
    
    @Test
	public void startsWithBlank(){
    	assertFalse(forPath("/bucket/file1").startsWith(forPath("")));
	}
    
    @Test
   	public void startsWithBlankRelative(){
       	assertFalse(forPath("file1").startsWith(forPath("")));
   	}
    
    @Test
   	public void startsWithBlankBlank(){
   		assertTrue(forPath("").startsWith(forPath("")));
   	}
    
    @Test
  	public void startsWithOnlyBuckets(){
  		assertTrue(forPath("/bucket").startsWith(forPath("/bucket")));
  	}
	
	@Test
	public void startsWithRelativeVsAbsolute(){
		assertFalse(forPath("/bucket/file1").startsWith(forPath("file1")));
	}
	
	@Test
	public void startsWithRelativeVsAbsoluteInBucket(){
		assertFalse(forPath("/bucket/file1").startsWith(forPath("bucket")));
	}
	
	@Test
	public void startsWithFalse(){
		assertFalse(forPath("/bucket/file1").startsWith(forPath("/bucket/file1/file2")));
		assertTrue(forPath("/bucket/file1/file2").startsWith(forPath("/bucket/file1")));
	}
	
	@Test
	public void startsWithNotNormalize(){
		assertFalse(forPath("/bucket/file1/file2").startsWith(forPath("/bucket/file1/../")));
	}
	
	@Test
	public void startsWithNormalize(){
		// in this implementation not exists .. or . special paths
		assertFalse(forPath("/bucket/file1/file2").startsWith(forPath("/bucket/file1/../").normalize()));
	}
	
	@Test
	public void startsWithRelative(){
		assertTrue(forPath("file/file1").startsWith(forPath("file")));
	}
   
    @Test
    public void startsWithDifferentProvider() {
    	assertFalse(forPath("/bucket/hello").startsWith(Paths.get("/bucket")));
    }
    
    @Test
    public void startsWithString(){
    	assertTrue(forPath("/bucket/hello").startsWith("/bucket/hello"));
    }
    
    @Test
    public void startsWithStringRelative(){
    	assertTrue(forPath("subkey1/hello").startsWith("subkey1/hello"));
    }
    
    @Test
  	public void startsWithStringOnlyBuckets(){
  		assertTrue(forPath("/bucket").startsWith("/bucket"));
  	}
	
	@Test
	public void startsWithStringRelativeVsAbsolute(){
		assertFalse(forPath("/bucket/file1").startsWith("file1"));
	}
    
    @Test
	public void startsWithStringFalse(){
		assertFalse(forPath("/bucket/file1").startsWith("/bucket/file1/file2"));
		assertTrue(forPath("/bucket/file1/file2").startsWith("/bucket/file1"));
	}
    
    @Test
	public void startsWithStringRelativeVsAbsoluteInBucket(){
		assertFalse(forPath("/bucket/file1").startsWith("bucket"));
	}
    
    // ends with
	
 	@Test
 	public void endsWithAbsoluteRelative(){
 		assertTrue(forPath("/bucket/file1").endsWith(forPath("file1")));
 	}
 	
 	@Test
 	public void endsWithAbsoluteAbsolute(){
 		assertTrue(forPath("/bucket/file1").endsWith(forPath("/bucket/file1")));
 	}
 	
 	@Test
 	public void endsWithRelativeRelative(){
 		assertTrue(forPath("file/file1").endsWith(forPath("file1")));
 	}
 	
 	@Test
 	public void endsWithRelativeAbsolute(){
 		assertFalse(forPath("file/file1").endsWith(forPath("/bucket")));
 	}
 	
 	@Test
 	public void endsWithDifferenteFileSystem(){
 		assertFalse(forPath("/bucket/file1").endsWith(Paths.get("/bucket/file1")));
 	}
 	
 	@Test
 	public void endsWithBlankRelativeAbsolute(){
 		assertFalse(forPath("").endsWith(forPath("/bucket")));
 	}
 	
 	@Test
	public void endsWithBlankBlank() {
		assertTrue(forPath("").endsWith(forPath("")));
	}
 	
 	@Test
 	public void endsWithRelativeBlankAbsolute(){
 		assertFalse(forPath("/bucket/file1").endsWith(forPath("")));
 	}
 	
 	@Test
 	public void endsWithRelativeBlankRelative(){
 		assertFalse(forPath("file1").endsWith(forPath("")));
 	}

    @Test
    public void endsWithDifferent(){
        assertFalse(forPath("/bucket/dir/dir/file1").endsWith(forPath("fail/dir/file1")));
    }

    @Test
    public void endsWithDifferentProvider() throws IOException {
        try (FileSystem linux = MemoryFileSystemBuilder.newLinux().build("linux")) {
            Path fileLinux = linux.getPath("/file");

            assertFalse(forPath("/bucket/file").endsWith(fileLinux));
        }

        try (FileSystem window = MemoryFileSystemBuilder.newWindows().build("window")) {
            Path file = window.getPath("c:/file");

            assertFalse(forPath("/c/file").endsWith(file));
        }
    }
 	
	@Test
 	public void endsWithString(){
		// endsWithAbsoluteRelative(){
 		assertTrue(forPath("/bucket/file1").endsWith("file1"));
		// endsWithAbsoluteAbsolute
	 	assertTrue(forPath("/bucket/file1").endsWith("/bucket/file1"));
		// endsWithRelativeRelative
 		assertTrue(forPath("file/file1").endsWith("file1"));
		// endsWithRelativeAbsolute
	 	assertFalse(forPath("file/file1").endsWith("/bucket"));
	 	// endsWithBlankRelativeAbsolute
	 	assertFalse(forPath("").endsWith("/bucket"));
		// endsWithBlankBlank
		assertTrue(forPath("").endsWith(""));
		// endsWithRelativeBlankAbsolute
		assertFalse(forPath("/bucket/file1").endsWith(""));
		// endsWithRelativeBlankRelative
 		assertFalse(forPath("file1").endsWith(""));
 	}
 	
 	// register
 	
 	@Test(expected = UnsupportedOperationException.class)
 	public void registerWithEventsThrowException() throws IOException{
 		forPath("file1").register(null, null);
 	}
 	
 	@Test(expected = UnsupportedOperationException.class)
 	public void registerThrowException() throws IOException{
 		forPath("file1").register(null);
 	}
 	
 	@Test(expected = UnsupportedOperationException.class)
 	public void registerWithEventsAndModiferThrowException() throws IOException{
 		forPath("file1").register(null, null, null);
 	}
 	
 	// to file
 	
 	@Test(expected = UnsupportedOperationException.class)
 	public void toFile() {
 		forPath("file1").toFile();
 	}
 	
 	// compares to
 	
 	@Test
 	public void compare() {
 		assertTrue(forPath("file1").compareTo(forPath("file1")) == 0);
 		assertTrue(forPath("/path/file1").compareTo(forPath("/path/file1")) == 0);
 		assertTrue(forPath("/A/file1").compareTo(forPath("/B/file1")) == -1);
 		assertTrue(forPath("/B/file1").compareTo(forPath("/A/file1")) == 1);
 		assertTrue(forPath("/AA/file1").compareTo(forPath("/A/file1")) > 0);
 		assertTrue(forPath("a").compareTo(forPath("aa")) < 0);
 		assertTrue(forPath("ab").compareTo(forPath("aa")) > 0);
 	}
 	
 	// toRealPath
 	
 	@Test(expected = UnsupportedOperationException.class)
 	public void toRealPathThrowException() throws IOException{
 		forPath("file1").toRealPath();
 	}
 	
 	// toAbsolutePath
 	
 	@Test(expected = IllegalStateException.class)
 	public void toAbsolutePathRelativePathThrowException() throws IOException{
 		forPath("file1").toAbsolutePath();
 	}
 	
 	@Test
 	public void toAbsolutePath() throws IOException{
 		Path path = forPath("/file1");
 		Path other = path.toAbsolutePath();
 		
 		assertEquals(path, other);
 	}
 	
 	// get root
 	
 	@Test
 	public void getRootReturnBucket(){
 		assertEquals(forPath("/bucketA"), forPath("/bucketA/dir/file").getRoot());
 	}
 	
 	@Test
 	public void getRootRelativeReturnNull(){
 		assertNull(forPath("dir/file").getRoot());
 	}

    // file name

    @Test
    public void getFileName(){
        Path path = forPath("/bucketA/file");
        Path name = path.getFileName();

        assertEquals(forPath("file"), name);
    }

    @Test
    public void getAnotherFileName(){
        Path path = forPath("/bucketA/dir/another-file");
        Path fileName = path.getFileName();
        Path dirName = path.getParent().getFileName();

        assertEquals(forPath("another-file"), fileName);
        assertEquals(forPath("dir"), dirName);
    }

    @Test
    public void getFileNameBucket(){
        Path path = forPath("/bucket");
        Path name = path.getFileName();

        assertNull(name);
    }

    // equals

    @Test
    public void equals(){
        Path path = forPath("/bucketA/dir/file");
        Path path2 = forPath("/bucketA/dir/file");

        assertTrue(path.equals(path2));
    }

    @Test
    public void notEquals(){
        Path path = forPath("/bucketA/dir/file");
        Path path2 = forPath("/bucketA/dir/file2");

        assertFalse(path.equals(path2));
    }

    @Test
    public void notEqualsNull(){
        Path path = forPath("/bucketA/dir/file");

        assertFalse(path.equals(null));
    }

    @Test
    public void notEqualsDifferentProvider() throws IOException {
        Path path = forPath("/c/dir/file");

        try (FileSystem linux = MemoryFileSystemBuilder.newLinux().build("linux")) {
            Path fileLinux = linux.getPath("/dir/file");

            assertFalse(path.equals(fileLinux));
        }

        try (FileSystem window = MemoryFileSystemBuilder.newWindows().build("window")) {
            Path file = window.getPath("c:/dir/file");

            assertFalse(path.equals(file));
        }
    }

    @Test
    public void hashCodeHashMap(){
        HashMap<S3Path, String> hashMap = new HashMap<>();
        hashMap.put(forPath("/bucket/a"), "a");
        hashMap.put(forPath("/bucket/a"), "b");

        assertEquals(1, hashMap.size());
        assertEquals("b", hashMap.get(forPath("/bucket/a")));
    }

 	
 	private static S3Path forPath(String path) {
 		return (S3Path)FileSystems.getFileSystem(URI
				.create("s3:///")).getPath(path);
 	}
}
