package com.crowdin.cli.properties.helper;

import com.crowdin.cli.properties.FileBean;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.utils.Utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.filefilter.RegexFileFilter;

/**
 * @author ihor
 */
public class FileHelper {

    private static final String FOLDER_SEPARATOR = (Utils.isWindows()) ? File.separator + File.separator : File.separator;

    private static final String DOUBLED_ASTERISK = "**";

    private static final String REGEX = "regex";

    private static final String ASTERISK = "*";

    private static final String QUESTION_MARK = "?";

    private static final String DOT = ".";

    private static final String DOT_PLUS = ".+";

    private static final String SET_OPEN_BRECKET = "[";

    private static final String SET_CLOSE_BRECKET = "]";

    private static final String ESCAPE_DOT = "\\.";

    private static final String ESCAPE_DOT_PLACEHOLDER = "{ESCAPE_DOT}";

    private static final String ESCAPE_QUESTION = "\\?";

    private static final String ESCAPE_QUESTION_PLACEHOLDER = "{ESCAPE_QUESTION_MARK}";

    private static final String ESCAPE_ASTERISK = "\\*";

    private static final String ESCAPE_ASTERISK_PLACEHOLDER = "{ESCAPE_ASTERISK}";

    public List<File> getFileSource(FileBean file, PropertiesBean propertiesBean) {
        List<File> resultList = new ArrayList<>();
        if (file != null) {
            String pattern = file.getSource();
            if (propertiesBean != null && propertiesBean.getBasePath() != null) {
                if (!propertiesBean.getBasePath().trim().endsWith(FOLDER_SEPARATOR) && !file.getSource().trim().startsWith(FOLDER_SEPARATOR)) {
                    pattern = propertiesBean.getBasePath() + FOLDER_SEPARATOR + file.getSource();
                } else {
                    pattern = propertiesBean.getBasePath().trim() + file.getSource().trim();
                    pattern = pattern.replaceAll(FOLDER_SEPARATOR + "+", FOLDER_SEPARATOR);
                }
            }
            pattern = pattern.replaceAll("\\\\+", "\\\\");
            pattern = pattern.replaceAll("/+", "/");
            String[] nodes = pattern.split(FOLDER_SEPARATOR);
            StringBuilder resultPath = new StringBuilder();
            for (String node : nodes) {
                if (!node.isEmpty()) {
                    if (resultList == null) {
                        break;
                    }
                    if (!DOUBLED_ASTERISK.equals(node)) {
                        node = translateToRegex(node);
                    }
                    if (DOUBLED_ASTERISK.equals(node)) {
                        resultList = filterFiles(DOUBLED_ASTERISK, resultList, node, resultPath);
                    } else if (node.contains(ASTERISK) || node.contains(QUESTION_MARK) || (node.contains(SET_OPEN_BRECKET) && node.contains(SET_CLOSE_BRECKET))) {
                        resultList = filterFiles(REGEX, resultList, node, resultPath);
                    } else {
                        resultList = filterFiles(REGEX, resultList, node, resultPath);
                    }
                }
            }
        }
        return resultList;
    }

    public List<File> getIgnoreFiles(FileBean file, PropertiesBean propertiesBean) {
        List<File> resultList = new ArrayList<>();
        if (file != null) {
            List<String> ignores = file.getIgnore();
            if (ignores != null) {
                for (String pattern : ignores) {
                    List<File> resultListTemp = new ArrayList<>();
                    if (propertiesBean != null && propertiesBean.getBasePath() != null) {
                        if (!propertiesBean.getBasePath().trim().endsWith(File.separator) && !pattern.trim().startsWith(File.separator)) {
                            pattern = propertiesBean.getBasePath() + File.separator + pattern;
                        } else {
                            pattern = propertiesBean.getBasePath().trim() + pattern.trim();
                            pattern = pattern.replace(File.separator + File.separator, File.separator);
                        }
                    }
                    String[] nodes = pattern.split(FOLDER_SEPARATOR);
                    StringBuilder resultPath = new StringBuilder();
                    for (String node : nodes) {
                        if (!node.isEmpty()) {
                            if (resultListTemp == null) {
                                break;
                            }
                            if (!DOUBLED_ASTERISK.equals(node)) {
                                node = translateToRegex(node);
                            }
                            if (DOUBLED_ASTERISK.equals(node)) {
                                resultListTemp = filterFiles(DOUBLED_ASTERISK, resultListTemp, node, resultPath);
                            } else if (node.contains(ASTERISK) || node.contains(QUESTION_MARK) || (node.contains(SET_OPEN_BRECKET) && node.contains(SET_CLOSE_BRECKET))) {
                                resultListTemp = filterFiles(REGEX, resultListTemp, node, resultPath);
                            } else {
                                resultListTemp = filterFiles(REGEX, resultListTemp, node, resultPath);
                            }
                        }
                    }
                    if (resultListTemp != null && resultListTemp.size() > 0) {
                        for (File f : resultListTemp) {
                            if (f.isFile()) {
                                resultList.add(f);
                            } else if (f.isDirectory()) {
                                resultList.addAll(this.getFilesFromDirectory(f));
                            } else {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        return resultList;
    }

    private String translateToRegex(String node) {
        if (node != null) {
            if (node.contains(DOT)) {
                if (node.contains(ESCAPE_DOT)) {
                    node = node.replace(ESCAPE_DOT, ESCAPE_DOT_PLACEHOLDER);
                }
                node = node.replace(DOT, ESCAPE_DOT);
                node = node.replace(ESCAPE_DOT_PLACEHOLDER, ESCAPE_DOT);
            }
            if (node.contains(QUESTION_MARK)) {
                if (node.contains(ESCAPE_QUESTION)) {
                    node = node.replace(ESCAPE_QUESTION, ESCAPE_QUESTION_PLACEHOLDER);
                }
                node = node.replace(QUESTION_MARK, DOT);
                node = node.replace(ESCAPE_QUESTION_PLACEHOLDER, ESCAPE_QUESTION);
            }
            if (node.contains(ASTERISK)) {
                if (node.contains(ESCAPE_ASTERISK)) {
                    node = node.replace(ESCAPE_ASTERISK, ESCAPE_ASTERISK_PLACEHOLDER);
                }
                node = node.replace(ASTERISK, DOT_PLUS);
                node = node.replace(ESCAPE_ASTERISK_PLACEHOLDER, ESCAPE_ASTERISK);
            }
        }
        return node;
    }

    private List<File> getFilesFromDirectory(File dir) {
        List<File> result = new ArrayList<>();
        if (dir != null) {
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            result.add(f);
                        } else if (f.isDirectory()) {
                            result.addAll(this.getFilesFromDirectory(f));
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    private List<File> filterFiles(String patternName, List<File> resultList, String node, StringBuilder resultPath) {
        if (!resultList.isEmpty()) {
            List<File> tmpResultList = new ArrayList<>();
            tmpResultList.addAll(resultList);
            resultList.clear();
            for (File file : tmpResultList) {
                StringBuilder absolutePath = new StringBuilder(file.getAbsolutePath());
                absolutePath.append(FOLDER_SEPARATOR);
                if (null != patternName) {
                    if (DOUBLED_ASTERISK.equals(patternName)) {
                        List<File> files = getlistDirectory(absolutePath.toString());
                        if (!files.isEmpty()) {
                            for (File f : files) {
                                File tmpFile = new File(f.getAbsolutePath());
                                if (tmpFile.isDirectory()) {
                                    resultList.add(tmpFile);
                                }
                            }
                        }
                    } else if (REGEX.equals(patternName)) {
                        File dir = new File(absolutePath.toString());
                        FileFilter fileFilter;
                        fileFilter = new RegexFileFilter(node);
                        File[] files = dir.listFiles(fileFilter);
                        if (files != null && files.length > 0) {
                            resultList.addAll(Arrays.asList(files));
                        }
                    } else {
                        absolutePath.append(node);
                        File tmpFile = new File(absolutePath.toString());
                        if (tmpFile.isFile() || tmpFile.isDirectory()) {
                            resultList.add(tmpFile);
                        }
                    }
                }
            }
        } else {
            if (node != null && node.endsWith(":")) {
                resultPath.append(node + FOLDER_SEPARATOR);
            } else {
                resultPath.append(FOLDER_SEPARATOR);
            }
            if (null != patternName) {
                if (DOUBLED_ASTERISK.equals(patternName)) {
                    List<File> files = getlistDirectory(resultPath.toString());
                    if (!files.isEmpty()) {
                        resultList.clear();
                        for (File f : files) {
                            File tmpF = new File(resultPath.toString() + FOLDER_SEPARATOR + f.getName());
                            if (tmpF.isDirectory()) {
                                resultList.add(tmpF);
                            }
                        }
                    }
                } else if (REGEX.equals(patternName)) {
                    File dir = new File(resultPath.toString());
                    FileFilter fileFilter;
                    fileFilter = new RegexFileFilter(node);
                    File[] files;
                    if (node != null && node.endsWith(":")) {
                        resultList.clear();
                        resultList.add(dir);
                        return resultList;
                    } else {
                        files = dir.listFiles(fileFilter);
                    }
                    if (files != null) {
                        resultList.clear();
                        resultList.addAll(Arrays.asList(files));
                    }
                } else {
                    resultPath.append(node);
                }
            } else {
                resultPath.append(node);
            }
        }
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList;
    }

    private List<File> getlistDirectory(String pathname) {
        File directory = new File(pathname);
        List<File> resultList = new ArrayList<>();
        resultList.add(directory);
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isDirectory()) {
                    resultList.addAll(getlistDirectory(file.getAbsolutePath()));
                }
            }
        }
        return resultList;
    }

}