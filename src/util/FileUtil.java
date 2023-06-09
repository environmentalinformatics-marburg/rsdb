package util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtil {
	
	private static class FolderSizeVisitor implements FileVisitor<Path> {
		public long size = 0;
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			size += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}		
	}

	public static long getFolderSize(Path path) throws IOException {
		FolderSizeVisitor fileSizeVisitor = new FolderSizeVisitor();
		Files.walkFileTree(path, fileSizeVisitor);
		return fileSizeVisitor.size;
	}
}
