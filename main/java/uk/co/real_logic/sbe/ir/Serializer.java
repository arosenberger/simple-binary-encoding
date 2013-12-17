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
import uk.co.real_logic.sbe.util.Verify;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class Serializer implements Closeable
{
    private static final int CAPACITY = 4096;

    private final FileChannel channel;
    private final ByteBuffer resultBuffer;
    private final ByteBuffer buffer;
    private final DirectBuffer directBuffer;
    private final IntermediateRepresentation ir;
    private final SerializedFrame serializedFrame = new SerializedFrame();
    private final SerializedToken serializedToken = new SerializedToken();
    private final byte[] valArray = new byte[CAPACITY];
    private final DirectBuffer valBuffer = new DirectBuffer(valArray);
    private int totalSize = 0;

    public Serializer(final String fileName, final IntermediateRepresentation ir)
        throws FileNotFoundException
    {
        channel = new FileOutputStream(fileName).getChannel();
        resultBuffer = null;
        buffer = ByteBuffer.allocateDirect(CAPACITY);
        directBuffer = new DirectBuffer(buffer);
        this.ir = ir;
    }

    public Serializer(final ByteBuffer buffer, final IntermediateRepresentation ir)
    {
        channel = null;
        resultBuffer = buffer;
        this.buffer = ByteBuffer.allocateDirect(CAPACITY);
        directBuffer = new DirectBuffer(this.buffer);
        this.ir = ir;
    }

    public void close()
        throws IOException
    {
        if (channel != null)
        {
            channel.close();
        }
    }

    public int serialize()
        throws IOException
    {
        Verify.notNull(ir, "ir");

        write(buffer, serializeFrame());

        serializeTokenList(ir.messageHeader().tokens());

        for (final List<Token> tokenList : ir.messages())
        {
            serializeTokenList(tokenList);
        }

        return totalSize;
    }

    private void serializeTokenList(final List<Token> tokenList)
        throws IOException
    {
        for (final Token token : tokenList)
        {
            write(buffer, serializeToken(token));
        }
    }

    private void write(final ByteBuffer buffer, final int size)
        throws IOException
    {
        buffer.position(0);
        buffer.limit(size);

        if (channel != null)
        {
            channel.write(buffer);
        }
        else if (resultBuffer != null)
        {
            resultBuffer.put(buffer);
        }

        totalSize += size;
    }

    private int serializeFrame()
    {
        serializedFrame.wrapForEncode(directBuffer, 0)
                       .sbeIrVersion(0)
                       .schemaVersion(ir.version());

        serializedFrame.putPackageVal(ir.packageName().getBytes(), 0, ir.packageName().getBytes().length);

        return serializedFrame.size();
    }

    private int serializeToken(final Token token)
    {
        final PrimitiveType type = token.encoding().primitiveType();

        serializedToken.wrapForEncode(directBuffer, 0)
                       .tokenOffset(token.offset())
                       .tokenSize(token.size())
                       .schemaID(token.schemaId())
                       .tokenVersion(token.version())
                       .signal(SerializationUtils.signal(token.signal()))
                       .primitiveType(SerializationUtils.primitiveType(type))
                       .byteOrder(SerializationUtils.byteOrder(token.encoding().byteOrder()));

        serializedToken.putName(token.name().getBytes(), 0, token.name().getBytes().length);

        serializedToken.putConstVal(valArray, 0, SerializationUtils.putVal(valBuffer, token.encoding().constVal(), type));
        serializedToken.putMinVal(valArray, 0, SerializationUtils.putVal(valBuffer, token.encoding().minVal(), type));
        serializedToken.putMaxVal(valArray, 0, SerializationUtils.putVal(valBuffer, token.encoding().maxVal(), type));
        serializedToken.putNullVal(valArray, 0, SerializationUtils.putVal(valBuffer, token.encoding().nullVal(), type));

        serializedToken.putCharacterEncoding(token.encoding().characterEncoding().getBytes(), 0,
                                             token.encoding().characterEncoding().getBytes().length);

        return serializedToken.size();
    }
}
