/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.Generated;
import com.hazelcast.client.impl.protocol.codec.builtin.*;
import com.hazelcast.client.impl.protocol.codec.custom.*;

import javax.annotation.Nullable;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/*
 * This file is auto-generated by the Hazelcast Client Protocol Code Generator.
 * To change this file, edit the templates or the protocol
 * definitions on the https://github.com/hazelcast/hazelcast-client-protocol
 * and regenerate it.
 */

/**
 * Applies the user defined EntryProcessor to the entry mapped by the key. Returns immediately with a Future
 * representing that task.EntryProcessor is not cancellable, so calling Future.cancel() method won't cancel the
 * operation of EntryProcessor.
 */
@Generated("6a8387fcbb8565107e1962fb4765c605")
public final class MapSubmitToKeyCodec {
    //hex: 0x012F00
    public static final int REQUEST_MESSAGE_TYPE = 77568;
    //hex: 0x012F01
    public static final int RESPONSE_MESSAGE_TYPE = 77569;
    private static final int REQUEST_THREAD_ID_FIELD_OFFSET = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int REQUEST_INITIAL_FRAME_SIZE = REQUEST_THREAD_ID_FIELD_OFFSET + LONG_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_BACKUP_ACKS_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;

    private MapSubmitToKeyCodec() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class RequestParameters {

        /**
         * name of map
         */
        public java.lang.String name;

        /**
         * entry processor to be executed on the entry.
         */
        public com.hazelcast.internal.serialization.Data entryProcessor;

        /**
         * the key of the map entry.
         */
        public com.hazelcast.internal.serialization.Data key;

        /**
         * Id of the thread that the task is submitted from.
         */
        public long threadId;
    }

    public static ClientMessage encodeRequest(java.lang.String name, com.hazelcast.internal.serialization.Data entryProcessor, com.hazelcast.internal.serialization.Data key, long threadId) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setContainsSerializedDataInRequest(true);
        clientMessage.setRetryable(false);
        clientMessage.setOperationName("Map.SubmitToKey");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeInt(initialFrame.content, PARTITION_ID_FIELD_OFFSET, -1);
        encodeLong(initialFrame.content, REQUEST_THREAD_ID_FIELD_OFFSET, threadId);
        clientMessage.add(initialFrame);
        StringCodec.encode(clientMessage, name);
        DataCodec.encode(clientMessage, entryProcessor);
        DataCodec.encode(clientMessage, key);
        return clientMessage;
    }

    public static MapSubmitToKeyCodec.RequestParameters decodeRequest(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        RequestParameters request = new RequestParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        request.threadId = decodeLong(initialFrame.content, REQUEST_THREAD_ID_FIELD_OFFSET);
        request.name = StringCodec.decode(iterator);
        request.entryProcessor = DataCodec.decode(iterator);
        request.key = DataCodec.decode(iterator);
        return request;
    }

    public static ClientMessage encodeResponse(@Nullable com.hazelcast.internal.serialization.Data response) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        clientMessage.add(initialFrame);

        CodecUtil.encodeNullable(clientMessage, response, DataCodec::encode);
        return clientMessage;
    }

    /**
     * result of entry process.
     */
    public static com.hazelcast.internal.serialization.Data decodeResponse(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        //empty initial frame
        iterator.next();
        return CodecUtil.decodeNullable(iterator, DataCodec::decode);
    }
}
