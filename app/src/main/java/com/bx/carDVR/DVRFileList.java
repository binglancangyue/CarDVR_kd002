package com.bx.carDVR;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.util.Log;

import com.bx.carDVR.bylym.model.tools.CheckStorageSpace;
import com.bx.carDVR.bylym.model.tools.SharePreferenceTool;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DVRFileList {
    public static final String LOG_TAG = "DVRFileList";

    private Map<String, List<String>> mVideoList;
    private Map<String, List<String>> mPicList;
    private File mVideoDirFile, mPicDirFile;
    private List<String> mLockedFiles;
    public static final String IMPACT_SUFFIX = "_impact";
    public static final String SUBFIX_1_MIN = "_1min";
    public static final String SUBFIX_3_MIN = "_3min";
    public static final String SUBFIX_5_MIN = "_5min";
    private int mCameraId;
    private SimpleDateFormat dateFormat;
    public static final double factor = 0.1;
    private CheckStorageSpace mCheckStorageSpace;
    private String SD_Path;

    @SuppressLint("SimpleDateFormat")
    public DVRFileList(DVRFileInfo fileInfo, int cameraID) {
        String path;
        this.mCameraId = cameraID;
        mCheckStorageSpace = new CheckStorageSpace();
        Log.d("DVRFileList", "DVRFileList: " + fileInfo.outputFilePath);
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        path = fileInfo.outputFilePath + "/" + DVRFileInfo.DIRECTORY_NAME +
                "/" + fileInfo.directoryID + "/" + DVRFileInfo.VIDEO_DIR_NAME;
        mVideoDirFile = new File(path);
        SD_Path = fileInfo.outputFilePath;

        path = fileInfo.outputFilePath + "/" + DVRFileInfo.DIRECTORY_NAME +
                "/" + fileInfo.directoryID + "/" + DVRFileInfo.PIC_DIR_NAME;
        mPicDirFile = new File(path);

        mVideoList = new HashMap<>();
        mPicList = new HashMap<>();
        mLockedFiles = new ArrayList<>();

        if (!mVideoDirFile.exists()) {
            mVideoDirFile.mkdirs();
        }

        if (!mPicDirFile.exists()) {
            mPicDirFile.mkdirs();
        }
    }

    public synchronized List<String> getLockedFiles() {
        return new ArrayList<>(mLockedFiles);
    }

    public synchronized void lockFile(String path) {
        mLockedFiles.add(path);
    }

    public synchronized Map<String, List<String>> getVideoList() {
        Map<String, List<String>> listMap = new HashMap<String, List<String>>();
        Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String key;
        List<String> value;

        while (iterator.hasNext()) {
            entry = iterator.next();
            key = entry.getKey();
            value = entry.getValue();
            listMap.put(key, new ArrayList<String>(value));
        }

        return listMap;
    }

    public synchronized Map<String, List<String>> getPictureList() {
        Map<String, List<String>> listMap = new HashMap<String, List<String>>();
        Iterator<Map.Entry<String, List<String>>> iterator = mPicList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String key;
        List<String> value;

        while (iterator.hasNext()) {
            entry = iterator.next();
            key = entry.getKey();
            value = entry.getValue();
            listMap.put(key, new ArrayList<String>(value));
        }

        return listMap;
    }

    public synchronized File newVideoFile() {
        return newVideoFile(true);
    }

    public synchronized File newPictureFile() {
        return newPictureFile(true);
    }

    private String createFileName(long dateTaken) {
        Date date = new Date(dateTaken);
        String backOrFront = null;

        SimpleDateFormat dateFormat = null;
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        if (mCameraId == Configuration.CAMERA_IDS[0]) {
            backOrFront = "_Front";
        } else if (mCameraId == Configuration.CAMERA_IDS[1]) {
            backOrFront = "_Back";
        }
        String shortCutSuffix = null;
        int mVideoDuration = SharePreferenceTool.getInstance().getRecordTime();
        Log.d("TAG", "mVideoDuration =" + mVideoDuration);
        if (mVideoDuration == 60) {
            shortCutSuffix = SUBFIX_1_MIN;
        } else if (mVideoDuration == 180) {
            shortCutSuffix = SUBFIX_3_MIN;
        } else {
            shortCutSuffix = SUBFIX_5_MIN;
        }
        return dateFormat.format(date) + backOrFront + shortCutSuffix + DVRFileInfo.VIDEO_FILE_EXT;
    }

    public synchronized File newVideoFile(boolean autoClean) {
        if (mVideoDirFile.exists()) {
            if (mVideoDirFile.canWrite() == false && mVideoDirFile.setWritable(true) == false) {
                Log.w(LOG_TAG, "can not write !");
                return null;
            }

            if (autoClean) {
                mCheckStorageSpace.checkStorageSpace(SD_Path, 0);
            }
        } else {
            Log.w(LOG_TAG, "file path is invalid !");
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        String dirName = String.format("%04d%02d%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
//        String fileName = DVRFileInfo.VIDEO_FILE_NAME_HEADER +
//                String.format("%04d%02d%02d%02d%02d%02d",
//                        calendar.get(Calendar.YEAR),
//                        calendar.get(Calendar.MONTH) + 1,
//                        calendar.get(Calendar.DAY_OF_MONTH),
//                        calendar.get(Calendar.HOUR_OF_DAY),
//                        calendar.get(Calendar.MINUTE),
//                        calendar.get(Calendar.SECOND)) +
//                DVRFileInfo.VIDEO_FILE_EXT;

        String fileName = createFileName(System.currentTimeMillis());

        File dirFile = new File(mVideoDirFile, dirName);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            Log.w(LOG_TAG, "Can not create folder !");
            return null;
        }
        Log.d("TAG", "newVideoFile: dirName " + dirName + " fileName " + fileName);
        File file = new File(dirFile, fileName);
        /*
        try {
            if (!file.createNewFile()) {
                Log.w(TAG, "Can not create file !");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "create video file fail !");
            return null;
        }
        */

/*        List<String> fileList = mVideoList.get(dirName);
        if (fileList == null) {
            fileList = new ArrayList<String>();
            mVideoList.put(dirName, fileList);
        }
        fileList.add(fileName);*/
        return file;
    }

    public synchronized File newPictureFile(boolean autoClean) {
        if (mPicDirFile.exists()) {
            if (mPicDirFile.canWrite() == false && mPicDirFile.setWritable(true) == false) {
                Log.w(LOG_TAG, "can not write !");
                return null;
            }

            if (autoClean) {
                mCheckStorageSpace.checkStorageSpace(SD_Path, 0);
            }
        } else {
            Log.w(LOG_TAG, "file path is invalid !");
            return null;
        }
        String backOrFront;
        if (mCameraId == Configuration.CAMERA_IDS[0]) {
            backOrFront = "_Front";
        } else {
            backOrFront = "_Back";
        }
        Calendar calendar = Calendar.getInstance();
        String dirName = String.format("%04d%02d%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
//        String fileName = DVRFileInfo.PIC_FILE_NAME_HEADER +
//                String.format("%04d%02d%02d%02d%02d%02d%03d",
//                        calendar.get(Calendar.YEAR),
//                        calendar.get(Calendar.MONTH) + 1,
//                        calendar.get(Calendar.DAY_OF_MONTH),
//                        calendar.get(Calendar.HOUR_OF_DAY),
//                        calendar.get(Calendar.MINUTE),
//                        calendar.get(Calendar.SECOND),
//                        calendar.get(Calendar.MILLISECOND)) + backOrFront +
//                DVRFileInfo.PIC_FILE_EXT;
        File dirFile = new File(mPicDirFile, dirName);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            Log.w(LOG_TAG, "Can not create folder !");
            return null;
        }

        String fileName = DVRFileInfo.PIC_FILE_NAME_HEADER + dateFormat.format(System.currentTimeMillis())
                + backOrFront + ".yuv";
        File file = new File(dirFile, fileName);

        /*
        try {
            if (!file.createNewFile()) {
                Log.w(TAG, "Can not create file !");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "create picture file fail !");
            return null;
        }
        */

/*        List<String> fileList = mPicList.get(dirName);
        if (fileList == null) {
            fileList = new ArrayList<String>();
            mPicList.put(dirName, fileList);
        }
        fileList.add(fileName);*/
        return file;
    }

    public static boolean deleteFolder(File dirFile) {
        File[] files = dirFile.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }

        return dirFile.delete();
    }

    public synchronized boolean deleteVideoDirectory(String name) {
        if (name == null) {
            return false;
        }

        Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String dirName;

        while (iterator.hasNext()) {
            entry = iterator.next();
            dirName = entry.getKey();

            if (dirName.equals(name)) {
                File dirFile = new File(mVideoDirFile, dirName);
                deleteFolder(dirFile);
                mVideoList.remove(dirName);
                return true;
            }
        }

        return false;
    }

    public synchronized boolean deletePictureDirectory(String name) {
        if (name == null) {
            return false;
        }

        Iterator<Map.Entry<String, List<String>>> iterator = mPicList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String dirName;

        while (iterator.hasNext()) {
            entry = iterator.next();
            dirName = entry.getKey();

            if (dirName.equals(name)) {
                File dirFile = new File(mPicDirFile, dirName);
                deleteFolder(dirFile);
                mPicList.remove(dirName);

                return true;
            }
        }

        return false;
    }

    public synchronized String findFilePath(String name) {
        if (name == null) {
            return null;
        }

        String ext = name.substring(name.lastIndexOf("."));
        if (ext.equals(DVRFileInfo.VIDEO_FILE_EXT) && name.contains(DVRFileInfo.VIDEO_FILE_NAME_HEADER)) {
            Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
            Map.Entry<String, List<String>> entry;
            String dirName;
            List<String> fileList;

            while (iterator.hasNext()) {
                entry = iterator.next();
                dirName = entry.getKey();
                fileList = entry.getValue();

                for (String item : fileList) {
                    if (item.equals(name)) {
                        return (mVideoDirFile.getPath() + "/" + dirName + "/" + name);
                    }
                }
            }
        } else if (ext.equals(DVRFileInfo.PIC_FILE_EXT) && name.contains(DVRFileInfo.PIC_FILE_NAME_HEADER)) {
            Iterator<Map.Entry<String, List<String>>> iterator = mPicList.entrySet().iterator();
            Map.Entry<String, List<String>> entry;
            String dirName;
            List<String> fileList;

            while (iterator.hasNext()) {
                entry = iterator.next();
                dirName = entry.getKey();
                fileList = entry.getValue();

                for (String item : fileList) {
                    if (item.equals(name)) {
                        return (mPicDirFile.getPath() + "/" + dirName + "/" + name);
                    }
                }
            }
        }

        return null;
    }

    public synchronized boolean deleteFile(String name) {
        if (name == null) {
            return false;
        }

        String ext = name.substring(name.lastIndexOf("."));
        if (ext.equals(DVRFileInfo.VIDEO_FILE_EXT) && name.contains(DVRFileInfo.VIDEO_FILE_NAME_HEADER)) {
            Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
            Map.Entry<String, List<String>> entry;
            String dirName;
            List<String> fileList;

            while (iterator.hasNext()) {
                entry = iterator.next();
                dirName = entry.getKey();
                fileList = entry.getValue();

                for (String item : fileList) {
                    if (item.equals(name)) {
                        File dirFile = new File(mVideoDirFile, dirName);
                        File file = new File(dirFile, name);

                        if (file.delete()) {
                            fileList.remove(item);
                        } else {
                            return false;
                        }

                        if (fileList.isEmpty()) {
                            dirFile.delete();
                            mVideoList.remove(dirName);
                        }
                        return true;
                    }
                }
            }
        } else if (ext.equals(DVRFileInfo.PIC_FILE_EXT) && name.contains(DVRFileInfo.PIC_FILE_NAME_HEADER)) {
            Iterator<Map.Entry<String, List<String>>> iterator = mPicList.entrySet().iterator();
            Map.Entry<String, List<String>> entry;
            String dirName;
            List<String> fileList;

            while (iterator.hasNext()) {
                entry = iterator.next();
                dirName = entry.getKey();
                fileList = entry.getValue();

                for (String item : fileList) {
                    if (item.equals(name)) {
                        File dirFile = new File(mPicDirFile, dirName);
                        File file = new File(dirFile, name);

                        if (file.delete()) {
                            fileList.remove(item);
                        } else {
                            return false;
                        }

                        if (fileList.isEmpty()) {
                            dirFile.delete();
                            mPicList.remove(dirName);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public synchronized boolean deleteEarliestVideo() {
        long timeMin = 0;
        String timeMinDir = null;
        String timeMinFile = null;
        List<String> timeMinList = null;

        Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String dirName;
        List<String> fileList;

        while (iterator.hasNext()) {
            entry = iterator.next();
            dirName = entry.getKey();
            fileList = entry.getValue();

            try {
                long time = Long.valueOf(dirName);
                if (timeMinDir == null || time < timeMin) {
                    timeMin = time;
                    timeMinDir = dirName;
                    timeMinList = fileList;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinDir == null || timeMinList == null) {
            Log.w(LOG_TAG, "no video directory !");
            return false;
        }

        for (String item : timeMinList) {
            String timeStr = item.substring(item.lastIndexOf(DVRFileInfo.VIDEO_FILE_NAME_HEADER) +
                    DVRFileInfo.VIDEO_FILE_NAME_HEADER.length(), item.lastIndexOf(".") - 1);

            try {
                long time = Long.valueOf(timeStr);
                if (timeMinFile == null || time < timeMin) {
                    timeMin = time;
                    timeMinFile = item;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinFile != null) {
            File dirFile = new File(mVideoDirFile, timeMinDir);
            File file = new File(dirFile, timeMinFile);
            file.delete();
            timeMinList.remove(timeMinFile);
            if (timeMinList.isEmpty()) {
                deleteFolder(dirFile);
                mVideoList.remove(timeMinDir);
            }
            return true;
        } else {
            Log.w(LOG_TAG, "empty video directory !");
        }

        return false;
    }

    public synchronized boolean deleteEarliestPicture() {
        long timeMin = 0;
        String timeMinDir = null;
        String timeMinFile = null;
        List<String> timeMinList = null;

        Iterator<Map.Entry<String, List<String>>> iterator = mPicList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String dirName;
        List<String> fileList;

        while (iterator.hasNext()) {
            entry = iterator.next();
            dirName = entry.getKey();
            fileList = entry.getValue();

            try {
                long time = Long.valueOf(dirName);
                if (timeMinDir == null || time < timeMin) {
                    timeMin = time;
                    timeMinDir = dirName;
                    timeMinList = fileList;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinDir == null || timeMinList == null) {
            Log.w(LOG_TAG, "no picture directory !");
            return false;
        }

        for (String item : timeMinList) {
            String timeStr = item.substring(item.lastIndexOf(DVRFileInfo.PIC_FILE_NAME_HEADER) +
                    DVRFileInfo.PIC_FILE_NAME_HEADER.length(), item.lastIndexOf(".") - 1);

            try {
                long time = Long.valueOf(timeStr);
                if (timeMinFile == null || time < timeMin) {
                    timeMin = time;
                    timeMinFile = item;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinFile != null) {
            File dirFile = new File(mPicDirFile, timeMinDir);
            File file = new File(dirFile, timeMinFile);
            file.delete();
            timeMinList.remove(timeMinFile);
            if (timeMinList.isEmpty()) {
                deleteFolder(dirFile);
                mPicList.remove(timeMinDir);
            }
            return true;
        } else {
            Log.w(LOG_TAG, "empty picture directory !");
        }
        return false;
    }

    public static long getFileLength(File file) {
        if (file == null)
            return -1;
        long size = 0;
        if (!file.isDirectory()) {
            size = file.length();
        } else {
            for (File f : file.listFiles()) {
                size += getFileLength(f);
            }
        }
        return size;
    }

    public synchronized long deleteEarliestVideoReturnBig() {
        long timeMin = 0;
        String timeMinDir = null;
        String timeMinFile = null;
        List<String> timeMinList = null;

        Iterator<Map.Entry<String, List<String>>> iterator = mVideoList.entrySet().iterator();
        Map.Entry<String, List<String>> entry;
        String dirName;
        List<String> fileList;

        while (iterator.hasNext()) {
            entry = iterator.next();
            dirName = entry.getKey();
            fileList = entry.getValue();

            try {
                long time = Long.valueOf(dirName);
                if (timeMinDir == null || time < timeMin) {
                    timeMin = time;
                    timeMinDir = dirName;
                    timeMinList = fileList;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinDir == null || timeMinList == null) {
            Log.w(LOG_TAG, "no video directory !");
            return -1;
        }

        for (String item : timeMinList) {
            String timeStr = item.substring(item.lastIndexOf(DVRFileInfo.VIDEO_FILE_NAME_HEADER) +
                    DVRFileInfo.VIDEO_FILE_NAME_HEADER.length(), item.lastIndexOf(".") - 1);

            try {
                long time = Long.valueOf(timeStr);
                if (timeMinFile == null || time < timeMin) {
                    timeMin = time;
                    timeMinFile = item;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if (timeMinFile != null) {
            File dirFile = new File(mVideoDirFile, timeMinDir);
            long result = getFileLength(dirFile) / 1024 / 1024;
            File file = new File(dirFile, timeMinFile);
            file.delete();
            timeMinList.remove(timeMinFile);
            if (timeMinList.isEmpty()) {
                deleteFolder(dirFile);
                mVideoList.remove(timeMinDir);
            }
            return result;
        } else {
            Log.w(LOG_TAG, "empty video directory !");
        }

        return -1;
    }


}
