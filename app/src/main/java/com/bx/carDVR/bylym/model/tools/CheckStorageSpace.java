package com.bx.carDVR.bylym.model.tools;

import android.annotation.SuppressLint;
import android.util.Log;

import com.bx.carDVR.DVRFileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class CheckStorageSpace {
    public static final long KEEP_STORAGE = 500;
    public static final long ADD_DELETE_STORAGE = 500;

    private ArrayList<File> files;
    private CompositeDisposable compositeDisposable;
    private int mType = 0;

    public CheckStorageSpace() {
        this.compositeDisposable = new CompositeDisposable();
    }

    // 递归遍历每个文件的大小
    public long getFileLength(File file) {
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

    @SuppressLint("UsableSpace")
    public void toCheckStorageSpace(String path, ObservableEmitter<String> emitter) {
        File file = new File(path);
        long usableSpace = file.getTotalSpace() / 1024 / 1024 - getFileLength(file)/1024/1024;
//        long totalSpace = file.getTotalSpace() / 1024 / 1024;
        Log.d("CheckStorageSpace", "usableSpace: " + usableSpace);
        if (usableSpace <= KEEP_STORAGE) {
            path = path + "/" + DVRFileInfo.DIRECTORY_NAME;
            Log.d("CheckStorageSpace", "toCheckStorageSpace: " + path);
            getFiles(path,KEEP_STORAGE-usableSpace+ADD_DELETE_STORAGE);
        }
        emitter.onNext("ok");
    }

    private void deleteFiles(long deleteSize) {
        Log.d("CheckStorageSpace", "deleteFiles start ");
        int size=0;
        for (File file : files) {
            String path = file.getAbsolutePath();
            if (!path.contains("impact") && path.contains(".mp4")) {
                Log.d("CheckStorageSpace", "deleteFiles: " + file.getAbsolutePath());
                size += file.length()/1024/1024;
                file.delete();
                if(size > deleteSize){
                    break;
                }
            }
        }
        Log.d("CheckStorageSpace", "deleteFiles over ");
    }

    private void getFiles(String path,long deleteSize) {
        File fileSDCard = new File(path);
        files = new ArrayList<>();
        if (fileSDCard.exists()) {
            if (fileSDCard.exists()) {
                refreshFileList(path, files);
            }
            //排序
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            deleteFiles(deleteSize);
        } else {
            Log.d("TAG", "getFiles: path " + path);
        }
    }

    private void refreshFileList(String strPath, ArrayList<File> freelist) {
        File dir = new File(strPath);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                refreshFileList(file.getAbsolutePath(), freelist);
            } else {
                freelist.add(file);
            }
        }
    }

    public void checkStorageSpace(String path, int type) {
        mType = type;
        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                toCheckStorageSpace(path, emitter);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String fileName) throws Exception {
                        Log.d("CheckStorageSpace", "CheckStorageSpace over ");
                    }
                }));
    }

}
