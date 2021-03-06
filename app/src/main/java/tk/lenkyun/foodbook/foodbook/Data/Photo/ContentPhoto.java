package tk.lenkyun.foodbook.foodbook.Data.Photo;

import android.net.Uri;

import tk.lenkyun.foodbook.foodbook.Data.Content;

/**
 * Created by lenkyun on 17/10/2558.
 */
public class ContentPhoto<E> extends Photo implements Content<E>{
    E content;

    public ContentPhoto(E content, Uri referalImageURI) {
        super(referalImageURI);
    }

    @Override
    public String getContentType(){
        return content.getClass().getName();
    }

    @Override
    public E getContent(){
        return this.content;
    }
    @Override
    public void setContent(E content){
        this.content = content;
    }

    public static <E> ContentPhoto<E> fromPhoto(Photo photo){
        return new ContentPhoto<E>(null, photo.getReferalImageURI());
    }

    public static <E> ContentPhoto<E> fromContent(E content){
        return new ContentPhoto<E>(content, null);
    }

    public static <E> ContentPhoto<E> fromContent(E content, Photo photo){
        return new ContentPhoto<E>(content, photo.getReferalImageURI());
    }

    public static <E> ContentPhoto<E> fromPhoto(Photo photo, E content){
        return new ContentPhoto<E>(content, photo.getReferalImageURI());
    }
}
