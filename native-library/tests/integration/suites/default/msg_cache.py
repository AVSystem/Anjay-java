# -*- coding: utf-8 -*-
#
# Copyright 2020-2021 AVSystem <avsystem@avsystem.com>
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
from framework.lwm2m.tlv import TLV
from framework.lwm2m_test import *
from framework.test_utils import *
from .test_object import OID, RID


class CacheTest(jni_test.LocalSingleServerTest,
                test_suite.Lwm2mDmOperations):
    def setUp(self):
        super().setUp(extra_cmdline_args=['--cache-size', '4096'])

        self.serv.set_timeout(timeout_s=1)

    def runTest(self):
        IID = 1
        self.write_resource(
            self.serv,
            oid=OID.Test,
            iid=IID,
            rid=RID.Test.Int,
            content=str(0))

        # execute IncrementInteger
        inc_req = Lwm2mExecute('/%d/%d/%d' %
                               (OID.Test, IID, RID.Test.IncrementInt))
        self.serv.send(inc_req)
        inc_res = self.serv.recv()
        self.assertMsgEqual(Lwm2mChanged.matching(inc_req)(), inc_res)

        result = self.read_resource(
            self.serv,
            oid=OID.Test,
            iid=IID,
            rid=RID.Test.Int,
            accept=coap.ContentFormat.TEXT_PLAIN)
        self.assertEqual(result.content, bytes("1", 'ascii'))

        # # retransmit Increment Integer
        self.serv.send(inc_req)
        # should receive identical response
        self.assertMsgEqual(inc_res, self.serv.recv())

        # Counter should not increment second time
        result = self.read_resource(
            self.serv,
            oid=OID.Test,
            iid=IID,
            rid=RID.Test.Int,
            accept=coap.ContentFormat.TEXT_PLAIN)
        self.assertEqual(result.content, bytes("1", 'ascii'))

        # a new Execute should increment it though
        self.execute_resource(
            self.serv,
            oid=OID.Test,
            iid=IID,
            rid=RID.Test.IncrementInt)

        result = self.read_resource(
            self.serv,
            oid=OID.Test,
            iid=IID,
            rid=RID.Test.Int,
            accept=coap.ContentFormat.TEXT_PLAIN)
        self.assertEqual(result.content, bytes("2", 'ascii'))
