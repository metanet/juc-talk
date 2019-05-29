package com.hazelcast.juctalk;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.util.Comparator;

import static com.hazelcast.juctalk.util.RandomUtil.randomInt;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsFirst;

/**
 * Represents photos posted by pet owners
 */
public class Photo implements DataSerializable {

    public static final Comparator<Photo> PHOTO_COMPARATOR = nullsFirst(comparingInt(Photo::getId));

    /**
     * Denotes the Pet owner that posts this photo.
     */
    private long fence;

    /**
     * Incremented each time a new photo is posted.
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
        return (fence == 0)
                ? "Photo{" + "id=" + id + ", fileName='" + fileName + '\'' + '}'
                : "Photo{" + "fence=" + fence + ", id=" + id + ", fileName='" + fileName + '\'' + '}';
    }

    public static int getNextId(Photo current) {
        return current != null ? current.getId() + 1 : 1;
    }

    public static String getRandomPhotoFileName(String pet) {
        return pet + (1 + randomInt(15)) + ".png";
    }

}
