/*
 * Copyright 2013 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.sbe.ir;

import uk.co.real_logic.sbe.PrimitiveType;
import uk.co.real_logic.sbe.codec.java.DirectBuffer;
import uk.co.real_logic.sbe.ir.generated.SerializedFrame;
import uk.co.real_logic.sbe.ir.generated.SerializedToken;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Deserializer implements Closeable
{
    private static final int CAPACITY = 4096;

    private final FileChannel channel;
    private final DirectBuffer directBuffer;
    private final SerializedFrame serializedFrame = new SerializedFrame();
    private final SerializedToken serializedToken = new SerializedToken();
    private int offset;
    private final long size;
    private String irPackageName = null;
    private List<Token> irHeader = null;
    private int irVersion = 0;
    private final byte[] valArray = new byte[CAPACITY];
    private final DirectBuffer valBuffer = new DirectBuffer(valArray);

    public Deserializer(final String fileName)
        throws IOException
    {
        channel = new RandomAccessFile(fileName, "r").getChannel();
        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        directBuffer = new DirectBuffer(buffer);
        size = channel.size();
        offset = 0;
    }

    public Deserializer(final ByteBuffer buffer)
    {
        channel = null;
        size = buffer.limit();
        directBuffer = new DirectBuffer(buffer);
        offset = 0;
    }

    public void close()
        throws IOException
    {
        if (channel != null)
        {
            channel.close();
        }
    }

    public IntermediateRepresentation deserialize()
        throws IOException
    {
        deserializeFrame();

        final List<Token> tokens = new ArrayList<>();
        while (offset < size)
        {
            final Token token = deserializeToken();

            // System.out.println(token.toString());
            tokens.add(token);
        }

        int i = 0, size = tokens.size();

        if (tokens.get(0).signal() == Signal.BEGIN_COMPOSITE)
        {
            i = captureHeader(tokens, 0);
        }

        final IntermediateRepresentation ir = new IntermediateRepresentation(irPackageName, irHeader, irVersion);

        for (; i < size; i++)
        {
            if (tokens.get(i).signal() == Signal.BEGIN_MESSAGE)
            {
                i = captureMessage(tokens, i, ir);
            }
        }

        return ir;
    }

    private int captureHeader(final List<Token> tokens, int index)
    {
        final List<Token> headerTokens = new ArrayList<>();

        Token token = tokens.get(index);
        headerTokens.add(token);
        do
        {
            token = tokens.get(++index);
            headerTokens.add(token);
        }
        while (Signal.END_COMPOSITE != token.signal());

        irHeader = headerTokens;

        return index;
    }

    private int captureMessage(final List<Token> tokens, int index, final IntermediateRepresentation ir)
    {
        final List<Token> messageTokens = new ArrayList<>();

        Token token = tokens.get(index);
        messageTokens.add(token);
        do
        {
            token = tokens.get(++index);
            messageTokens.add(token);
        }
        while (Signal.END_MESSAGE != token.signal());

        ir.addMessage(tokens.get(index).schemaId(), messageTokens);

        return index;
    }

    private void deserializeFrame()
    {
        serializedFrame.wrapForDecode(directBuffer, offset, serializedFrame.blockLength(), 0);

        if (serializedFrame.sbeIrVersion() != 0)
        {
            // TODO: throw exception since we don't know how to handle this
        }

        final byte[] byteArray = new byte[1024];

        irVersion = serializedFrame.schemaVersion();

        irPackageName = new String(byteArray, 0, serializedFrame.getPackageVal(byteArray, 0, byteArray.length));

        offset += serializedFrame.size();
    }

    private Token deserializeToken()
    {
        final Token.Builder builder = new Token.Builder();
        final Encoding.Builder encBuilder = new Encoding.Builder();

        final byte[] byteArray = new byte[1024];

        serializedToken.wrapForDecode(directBuffer, offset, serializedToken.blockLength(), 0);

        builder.offset(serializedToken.tokenOffset())
               .size(serializedToken.tokenSize())
               .schemaId(serializedToken.schemaID())
               .version(serializedToken.tokenVersion())
               .signal(SerializationUtils.signal(serializedToken.signal()));

        final PrimitiveType type = SerializationUtils.primitiveType(serializedToken.primitiveType());

        encBuilder.primitiveType(type)
                  .byteOrder(SerializationUtils.byteOrder(serializedToken.byteOrder()));

        // must deserialize vardata in order

        builder.name(new String(byteArray, 0, serializedToken.getName(byteArray, 0, byteArray.length)));

        encBuilder.constVal(SerializationUtils.getVal(valBuffer, type, serializedToken.getConstVal(valArray, 0, valArray.length)));
        encBuilder.minVal(SerializationUtils.getVal(valBuffer, type, serializedToken.getMinVal(valArray, 0, valArray.length)));
        encBuilder.maxVal(SerializationUtils.getVal(valBuffer, type, serializedToken.getMaxVal(valArray, 0, valArray.length)));
        encBuilder.nullVal(SerializationUtils.getVal(valBuffer, type, serializedToken.getNullVal(valArray, 0, valArray.length)));

        encBuilder.characterEncoding(new String(byteArray, 0, serializedToken.getCharacterEncoding(byteArray, 0, byteArray.length)));

        offset += serializedToken.size();

        return builder.encoding(encBuilder.build()).build();
    }
}
