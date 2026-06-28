package com.ampoti.filemanager

import com.github.junrar.Archive
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ArchiveHelper {

    fun isSupportedArchive(extension: String): Boolean {
        return listOf("zip", "rar", "7z", "tar", "gz").contains(extension)
    }

    fun extractArchive(inputFile: File, outputDir: File): Boolean {
        return try {
            val fileName = inputFile.name.lowercase()
            when {
                fileName.endsWith(".rar") -> extractRar(inputFile, outputDir)
                fileName.endsWith(".7z") -> extract7z(inputFile, outputDir)
                fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> extractTarGz(inputFile, outputDir)
                fileName.endsWith(".gz") -> extractGz(inputFile, outputDir)
                else -> extractCommonsCompress(inputFile, outputDir)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractTarGz(inputFile: File, outputDir: File) {
        BufferedInputStream(FileInputStream(inputFile)).use { bis ->
            GzipCompressorInputStream(bis).use { gzis ->
                val ais: ArchiveInputStream<out ArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(gzis)
                var entry: ArchiveEntry? = ais.nextEntry
                while (entry != null) {
                    val outFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (ais.read(buffer).also { read = it } != -1) {
                                bos.write(buffer, 0, read)
                            }
                        }
                    }
                    entry = ais.nextEntry
                }
            }
        }
    }

    private fun extractGz(inputFile: File, outputDir: File) {
        BufferedInputStream(FileInputStream(inputFile)).use { bis ->
            GzipCompressorInputStream(bis).use { gzis ->
                val outFileName = inputFile.name.substring(0, inputFile.name.length - 3) // remove .gz
                val outFile = File(outputDir, outFileName)
                outFile.parentFile?.mkdirs()
                BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (gzis.read(buffer).also { read = it } != -1) {
                        bos.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun extractRar(inputFile: File, outputDir: File) {
        Archive(inputFile).use { archive ->
            var header = archive.nextFileHeader()
            while (header != null) {
                val outFile = File(outputDir, header.fileNameString)
                if (header.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        archive.extractFile(header, fos)
                    }
                }
                header = archive.nextFileHeader()
            }
        }
    }

    private fun extract7z(inputFile: File, outputDir: File) {
        SevenZFile(inputFile).use { sevenZFile ->
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                val outFile = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (sevenZFile.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                entry = sevenZFile.nextEntry
            }
        }
    }

    private fun extractCommonsCompress(inputFile: File, outputDir: File) {
        BufferedInputStream(FileInputStream(inputFile)).use { bis ->
            val ais: ArchiveInputStream<out ArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(bis)
            var entry: ArchiveEntry? = ais.nextEntry
            while (entry != null) {
                val outFile = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (ais.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                    }
                }
                entry = ais.nextEntry
            }
        }
    }

    // Future expansion: compression support
    fun compressFilesToZip(sourceFiles: List<File>, outputFile: File): Boolean {
        return try {
            ZipArchiveOutputStream(FileOutputStream(outputFile)).use { zos ->
                for (file in sourceFiles) {
                    addFileToZip(zos, file, "")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(zos: ZipArchiveOutputStream, file: File, parentPath: String) {
        val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
        if (file.isDirectory) {
            val entry = ZipArchiveEntry("$entryName/")
            zos.putArchiveEntry(entry)
            zos.closeArchiveEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(zos, child, entryName)
            }
        } else {
            val entry = ZipArchiveEntry(entryName)
            zos.putArchiveEntry(entry)
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeArchiveEntry()
        }
    }
}
