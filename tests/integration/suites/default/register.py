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

import socket

import jni_test
from framework.lwm2m.messages import *
from framework import test_suite
from framework.test_utils import *

class RegisterTest(jni_test.LocalSingleServerTest,
                   test_suite.Lwm2mDmOperations):
    def runTest(self):
        # HACK: this is done to delay CTRL+C being sent to the client before it realizes it registered properly.
        self.discover(self.serv, oid=OID.Server)
