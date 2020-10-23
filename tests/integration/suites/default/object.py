# -*- coding: utf-8 -*-
#
# Copyright 2020 AVSystem <avsystem@avsystem.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import jni_test
from framework.lwm2m.messages import *
from framework import test_suite
from framework.test_utils import *
from framework.lwm2m.senml_cbor import *

class OID:
    Test = 1337


class RID:
    class Test:
        Int = 0
        Long = 1
        Float = 2
        Double = 3
        String = 4
        Objlnk = 5
        Bytes = 6
        Executable = 7
        LastExecuteArgs = 8
        MultipleResource = 9


class TestObjectReadWrite(jni_test.LocalSingleServerTest,
                          test_suite.Lwm2mDmOperations):
    def test_read_write(self, rid, value):
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=rid, content=str(value))
        result = self.read_resource(self.serv, oid=OID.Test, iid=1, rid=rid, accept=coap.ContentFormat.TEXT_PLAIN)
        self.assertEqual(result.content, bytes(str(value), 'ascii'))

    def runTest(self):
        self.test_read_write(rid=RID.Test.Int, value=32)
        self.test_read_write(rid=RID.Test.Long, value=42)
        self.test_read_write(rid=RID.Test.Float, value=4.5)
        self.test_read_write(rid=RID.Test.Double, value=4.25)
        self.test_read_write(rid=RID.Test.String, value='wohoo')
        self.test_read_write(rid=RID.Test.Objlnk, value='22:38')
        self.test_read_write(rid=RID.Test.Bytes, value='YWJjZGUK') # abcde in base64


class TestObjectExecute(jni_test.LocalSingleServerTest,
                        test_suite.Lwm2mDmOperations):
    def runTest(self):
        self.execute_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Executable, content=b"0='3',1='5'")
        self.assertEquals(self.read_resource_instance(self.serv, oid=OID.Test, iid=1, rid=RID.Test.LastExecuteArgs, riid=0).content, b'3')
        self.assertEquals(self.read_resource_instance(self.serv, oid=OID.Test, iid=1, rid=RID.Test.LastExecuteArgs, riid=1).content, b'5')


class TestObjectMultipleResourceInstance(jni_test.LocalSingleServerTest,
                                         test_suite.Lwm2mDmOperations):
    def runTest(self):
        request = [
            {
                SenmlLabel.NAME: '/%d/1/%d/0' % (OID.Test, RID.Test.MultipleResource),
                SenmlLabel.VALUE: 32
            },
            {
                SenmlLabel.NAME: '/%d/1/%d/10' % (OID.Test, RID.Test.MultipleResource),
                SenmlLabel.VALUE: 31
            }
        ]
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.MultipleResource, content=CBOR.serialize(request), format=coap.ContentFormat.APPLICATION_LWM2M_SENML_CBOR)
        self.assertEquals(self.read_resource_instance(self.serv, oid=OID.Test, iid=1, rid=RID.Test.MultipleResource, riid=0).content, b'32')
        self.assertEquals(self.read_resource_instance(self.serv, oid=OID.Test, iid=1, rid=RID.Test.MultipleResource, riid=10).content, b'31')

