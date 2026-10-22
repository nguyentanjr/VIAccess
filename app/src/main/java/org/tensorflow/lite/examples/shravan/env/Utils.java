package org.tensorflow.lite.examples.shravan.env;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/** Utility class for model loading and other miscellaneous tasks. */
public class Utils {
  /** Memory-map the model file in Assets. */
  public static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
      throws IOException {
    AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  public static float expit(final float x) {
    return (float) (1. / (1. + Math.exp(-x)));
  }
}
