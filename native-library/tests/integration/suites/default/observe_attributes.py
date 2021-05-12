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
import socket
import time
import unittest

from framework.lwm2m_test import *

from .object import OID, RID


class ObserveAttributesTest(jni_test.LocalSingleServerTest,
                            test_suite.Lwm2mDmOperations):
    def runTest(self):
        # Initialize resource
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, content="5")

        # Observe: Counter
        counter_pkt = self.observe(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int)

        # no message should arrive here
        with self.assertRaises(socket.timeout):
            self.serv.recv(timeout_s=5)

        # Attribute invariants
        self.write_attributes(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, query=['st=-1'],
                              expect_error_code=coap.Code.RES_BAD_REQUEST)
        self.write_attributes(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int,
                              query=['lt=9', 'gt=4'], expect_error_code=coap.Code.RES_BAD_REQUEST)
        self.write_attributes(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int,
                              query=['lt=4', 'gt=9', 'st=3'],
                              expect_error_code=coap.Code.RES_BAD_REQUEST)

        # unparsable attributes
        self.write_attributes(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int,
                              query=['lt=invalid'], expect_error_code=coap.Code.RES_BAD_OPTION)

        # Write Attributes
        self.write_attributes(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int,
                              query=['pmax=2'])

        # now we should get notifications, even though nothing changed
        pkt = self.serv.recv(timeout_s=3)
        self.assertEqual(pkt.code, coap.Code.RES_CONTENT)
        self.assertEqual(pkt.content, counter_pkt.content)

        # and another one
        pkt = self.serv.recv(timeout_s=3)
        self.assertEqual(pkt.code, coap.Code.RES_CONTENT)
        self.assertEqual(pkt.content, counter_pkt.content)


class ObserveResourceInvalidPmax(jni_test.LocalSingleServerTest,
                                 test_suite.Lwm2mDmOperations):
    def runTest(self):
        # Initialize resource
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, content="5")

        # Set invalid pmax (smaller than pmin)
        self.write_attributes(
            self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, query=['pmin=2', 'pmax=1'])

        self.observe(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int)

        # No notification should arrive
        with self.assertRaises(socket.timeout):
            print(self.serv.recv(timeout_s=3))


class ObserveResourceZeroPmax(jni_test.LocalSingleServerTest,
                              test_suite.Lwm2mDmOperations):
    def runTest(self):
        # Initialize resource
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, content="5")

        # Set invalid pmax (equal to 0)
        self.write_attributes(
            self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, query=['pmax=0'])

        self.observe(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int)

        # No notification should arrive
        with self.assertRaises(socket.timeout):
            print(self.serv.recv(timeout_s=2))


class ObserveWithDefaultAttributesTest(jni_test.LocalSingleServerTest,
                                       test_suite.Lwm2mDmOperations):
    def runTest(self):
        # Initialize resource
        self.write_resource(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int, content="5")

        # Observe: Counter
        counter_pkt = self.observe(self.serv, oid=OID.Test, iid=1, rid=RID.Test.Int)
        # no message should arrive here
        with self.assertRaises(socket.timeout):
            self.serv.recv(timeout_s=5)

        # Attributes set via public API
        self.communicate('set-attrs /%d/%d/%d 1 pmax=1 pmin=1' % (OID.Test, 1, RID.Test.Int))
        # And should now start arriving each second
        pkt = self.serv.recv(timeout_s=2)
        self.assertEqual(pkt.code, coap.Code.RES_CONTENT)
        self.assertEqual(pkt.content, counter_pkt.content)
        # Up until they're reset
        self.communicate('set-attrs /%d/%d/%d 1' % (OID.Test, 1, RID.Test.Int))
