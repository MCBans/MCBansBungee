package com.mcbans.utils;


import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ObjectSerializer {

  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = null;
    byte[] yourBytes;
    try {
      out = new ObjectOutputStream(bos);
      out.writeObject(obj);
      out.flush();
      yourBytes = bos.toByteArray();
    } finally {
      out.close();
      bos.close();
    }
    return yourBytes;
  }

  public static <T> T deserialize(byte[] byteArray) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
    ObjectInput in = null;
    Object o;
    try {
      in = new ObjectInputStream(bis);
      o = in.readObject();
    } finally {
      if (in != null) {
        bis.close();
        in.close();
      }
    }
    return (T) o;
  }

  public static <T> byte[] serializeUsingBukkit(List<T> objects) throws IOException {
    return serialize(objects);
  }
  public static <T> List<T> deserializeUsingBukkit(byte[] byteObject) throws IOException, ClassNotFoundException {
    return deserialize(byteObject);
  }
}
