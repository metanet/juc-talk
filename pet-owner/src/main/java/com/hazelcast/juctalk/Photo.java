package com.hazelcast.juctalk;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.util.Comparator;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsFirst;

/**
 * Represents photos published by pet owners
 */
public class Photo implements DataSerializable {

    public static final Comparator<Photo> PHOTO_COMPARATOR = nullsFirst(comparingInt(Photo::getId));

    /**
     * The Pet owner that publishes this photo
     */
    private long fence;

    /**
     * The id of this photo. Incremented each time a new photo is published.
     */
    private int id;

    /**
     * File name of the photo
     */
    private String fileName;

    public Photo() {
    }

    public Photo(int id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    public Photo(long fence, int id, String fileName) {
        this.fence = fence;
        this.id = id;
        this.fileName = fileName;
    }

    public long getFence() {
        return fence;
    }

    public int getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(fence);
        out.writeInt(id);
        out.writeUTF(fileName);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        fence = in.readLong();
        id = in.readInt();
        fileName = in.readUTF();
    }

    @Override
    public String toString() {
        return "Photo{" + "fence=" + fence + ", id=" + id + ", fileName='" + fileName + '\'' + '}';
    }
}
