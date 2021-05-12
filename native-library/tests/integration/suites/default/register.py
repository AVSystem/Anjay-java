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

import socket
import os
import tempfile
import datetime

import jni_test
from framework.lwm2m.messages import *
from framework import test_suite
from framework.test_utils import *


class RegisterTest(jni_test.LocalSingleServerTest,
                   test_suite.Lwm2mDmOperations):
    def runTest(self):
        # HACK: this is done to delay CTRL+C being sent to the client before it
        # realizes it registered properly.
        self.discover(self.serv, oid=OID.Server)


class PskRegisterTest(RegisterTest):
    def setUp(self):
        super().setUp(psk_identity=b'identity', psk_key=b'psk')


class CertRegisterTest(RegisterTest):
    @staticmethod
    def _generate_key():
        from cryptography.hazmat.backends import default_backend
        from cryptography.hazmat.primitives.asymmetric import ec
        return ec.generate_private_key(
            ec.SECP384R1(), backend=default_backend())

    @staticmethod
    def _generate_cert(private_key, public_key, issuer_cn, cn='127.0.0.1'):
        import datetime
        import ipaddress
        from cryptography import x509
        from cryptography.x509.oid import NameOID
        from cryptography.hazmat.backends import default_backend
        from cryptography.hazmat.primitives import hashes

        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, cn)])
        issuer_name = x509.Name(
            [x509.NameAttribute(NameOID.COMMON_NAME, issuer_cn)])
        now = datetime.datetime.utcnow()
        cert_builder = (x509.CertificateBuilder().
                        subject_name(name).
                        issuer_name(issuer_name).
                        public_key(public_key).
                        serial_number(1000).
                        not_valid_before(now).
                        not_valid_after(now + datetime.timedelta(days=1)))
        return cert_builder.sign(
            private_key, hashes.SHA256(), default_backend())

    def generate_certs_and_keys(self):
        from cryptography.hazmat.primitives import serialization

        self.server_key_path = self.temp_dir.name + "/key.pem"
        self.server_cert_path = self.temp_dir.name + "/cert.pem"
        self.client_key_path = self.temp_dir.name + "/client-key.der"
        self.client_cert_path = self.temp_dir.name + "/client-cert.der"

        server_key = self._generate_key()
        with open(self.server_key_path, "wb") as f:
            f.write(
                server_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.TraditionalOpenSSL,
                    encryption_algorithm=serialization.NoEncryption(),
                ))

        server_cert = self._generate_cert(
            server_key, server_key.public_key(), '127.0.0.1')
        with open(self.server_cert_path, "wb") as f:
            f.write(server_cert.public_bytes(serialization.Encoding.PEM))

        client_key = self._generate_key()
        with open(self.client_key_path, "wb") as f:
            f.write(
                client_key.private_bytes(
                    encoding=serialization.Encoding.DER,
                    format=serialization.PrivateFormat.TraditionalOpenSSL,
                    encryption_algorithm=serialization.NoEncryption(),
                ))

        client_cert = self._generate_cert(
            client_key, client_key.public_key(), '127.0.0.1')
        with open(self.client_cert_path, "wb") as f:
            f.write(client_cert.public_bytes(serialization.Encoding.DER))

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.generate_certs_and_keys()
        super().setUp(
            server_crt_file=self.server_cert_path,
            server_key_file=self.server_key_path,
            extra_cmdline_args=[
                '-C',
                self.client_cert_path,
                '-K',
                self.client_key_path, ])
