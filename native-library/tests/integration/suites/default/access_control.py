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

from framework.lwm2m.tlv import TLVType
from framework.lwm2m_test import *

from . import bootstrap_server

import jni_test
import unittest

# In the following test suite we assume that self.servers[0] has SSID=1, and
# self.servers[1] has SSID=2. Current implementation of the demo guarantees
# that at least.
#
# Also SSID=2 is the master, and SSID=1 is his slave, it can do things allowed
# only by SSID=2, and this test set shall check that this is indeed the case.

g = SequentialMsgIdGenerator(1)

# We'd love to keep instance id in some reasonable bound before we dive into
# a solution that makes use of discover and so on.
IID_BOUND = 64


# This is all defined in standard
class AccessMask:
    NONE = 0
    READ = 1 << 0
    WRITE = 1 << 1
    EXECUTE = 1 << 2
    DELETE = 1 << 3
    CREATE = 1 << 4

    OWNER = READ | WRITE | EXECUTE | DELETE



def make_acl_entry(oid, iid, ssid, access_mask):
    return f'/{oid}/{iid},{ssid},{access_mask}'


class AccessControl:
    # not declaring the helper class in global scope to prevent it from being
    # considered a test case on its own
    class Test(bootstrap_server.BootstrapServer.Test, test_suite.Lwm2mDmOperations):
        def add_server(self, iid):
            server = Lwm2mServer()
            uri = 'coap://127.0.0.1:%d' % server.get_listen_port()
            self.write_instance(self.bootstrap_server, OID.Security, iid,
                                TLV.make_resource(RID.Security.ServerURI, uri).serialize()
                                + TLV.make_resource(RID.Security.Bootstrap, 0).serialize()
                                + TLV.make_resource(RID.Security.Mode, 3).serialize()
                                + TLV.make_resource(RID.Security.ShortServerID, iid).serialize()
                                + TLV.make_resource(RID.Security.PKOrIdentity, "").serialize()
                                + TLV.make_resource(RID.Security.SecretKey, "").serialize())
            self.write_instance(self.bootstrap_server, OID.Server, iid,
                                TLV.make_resource(RID.Server.Lifetime, 86400).serialize()
                                + TLV.make_resource(RID.Server.Binding, "U").serialize()
                                + TLV.make_resource(RID.Server.ShortServerID, iid).serialize()
                                + TLV.make_resource(RID.Server.NotificationStoring, True).serialize())
            return server

        def validate_iid(self, iid):
            self.assertTrue(iid >= 0 and iid < IID_BOUND)

        def find_access_control_instance(self, server, oid, iid, expect_existence=True):
            self.validate_iid(iid)
            # It is very sad, that we have to iterate through
            # Access Control instances, but we really do.
            for instance in range(IID_BOUND):
                req = Lwm2mRead(ResPath.AccessControl[instance].TargetIID)
                server.send(req)
                res = server.recv()

                # TODO: assertMsgEqual(Lwm2mResponse.matching...)
                if res.code != coap.Code.RES_CONTENT:
                    continue

                res = self.read_resource(server, OID.AccessControl, instance,
                                         RID.AccessControl.TargetOID)
                ret_oid = int(res.content)

                res = self.read_resource(server, OID.AccessControl, instance,
                                         RID.AccessControl.TargetIID)
                ret_iid = int(res.content)

                if ret_oid == oid and ret_iid == iid:
                    return instance
            if expect_existence:
                assert False, "%d/%d/%d does not exist" % (OID.AccessControl, oid, iid)
            return None

        def prepare_servers(self, acl_entries):
            self.bootstrap_server.connect_to_client(('127.0.0.1', self.get_demo_port()))

            # Bootstrap Delete /
            req = Lwm2mDelete('/')
            self.bootstrap_server.send(req)
            self.assertMsgEqual(Lwm2mDeleted.matching(req)(),
                                self.bootstrap_server.recv())

            # create servers
            self.servers = [self.add_server(1), self.add_server(2)]

            # create ACLs
            for acl_entry in acl_entries:
                self.communicate('set-acl ' + acl_entry)

            # check that those are the only ACLs currently in data model
            self.assertIn(
                b'</%d>,</%d/0>' % (OID.AccessControl, OID.AccessControl),
                self.discover(self.bootstrap_server).content)

            # send Bootstrap Finish
            req = Lwm2mBootstrapFinish()
            self.bootstrap_server.send(req)
            self.assertMsgEqual(Lwm2mChanged.matching(req)(),
                                self.bootstrap_server.recv())
            self.assertDemoRegisters(self.servers[0])
            self.assertDemoRegisters(self.servers[1])
            

class RemovingAcoInstanceFailsTest(AccessControl.Test):
    def runTest(self):
        self.prepare_servers(acl_entries=[make_acl_entry(1337, 1, 1, AccessMask.READ)])
        
        ac_iid = self.find_access_control_instance(self.servers[1], oid=1337, iid=1)
        self.delete_instance(server=self.servers[1], oid=OID.AccessControl, iid=ac_iid,
                             expect_error_code=coap.Code.RES_UNAUTHORIZED)


class AclBootstrapping(AccessControl.Test):
    def runTest(self):
        self.prepare_servers(acl_entries=[make_acl_entry(1337, 1, 1, AccessMask.READ | AccessMask.WRITE),
                                          make_acl_entry(1337, 1, 2, AccessMask.NONE)])

        # SSID 1 rights
        self.write_resource(server=self.servers[0], oid=1337, iid=1, rid=2, content='21.37')
        self.read_resource(server=self.servers[0], oid=1337, iid=1, rid=2)

        # SSID 2 rights
        self.write_resource(server=self.servers[1], oid=1337, iid=1, rid=2, content='37.21',
                            expect_error_code=coap.Code.RES_UNAUTHORIZED)
        self.read_resource(server=self.servers[1], oid=1337, iid=1, rid=2,
                           expect_error_code=coap.Code.RES_UNAUTHORIZED)
