package com.nimbits.server.zip.impl;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


public class CompressionImpl  {



    public static byte[] compress(byte[] input) throws UnsupportedEncodingException, IOException
    {

        Deflater df = new Deflater();       //this function mainly generate the byte code
        df.setLevel(Deflater.BEST_COMPRESSION);
        df.setInput(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);   //we write the generated byte code in this array
        df.finish();
        byte[] buff = new byte[1024];   //segment segment pop....segment set 1024
        while(!df.finished())
        {
            int count = df.deflate(buff);       //returns the generated code... index
            baos.write(buff, 0, count);     //write 4m 0 to count
        }
        baos.close();

        int baosLength = baos.toByteArray().length;
        int inputLength = input.length;
        //System.out.println("Original: "+inputLength);
        // System.out.println("Compressed: "+ baosLength);

        return baos.toByteArray();

    }


    public static byte[] decompress(byte[] input) throws UnsupportedEncodingException, IOException, DataFormatException
    {

        Inflater decompressor = new Inflater();
        decompressor.setInput(input);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Decompress the data
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
            }
        }
        try {
            bos.close();
        } catch (IOException e) {
        }

        // Get the decompressed data
        byte[] decompressedData = bos.toByteArray();

        return decompressedData;


    }



}
