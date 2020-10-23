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

import http
import os
import ssl
import threading
import time

import jni_test
from framework.lwm2m.messages import *
from framework.test_utils import *

UPDATE_STATE_IDLE = 0
UPDATE_STATE_DOWNLOADING = 1
UPDATE_STATE_DOWNLOADED = 2
UPDATE_STATE_UPDATING = 3

UPDATE_RESULT_INITIAL = 0
UPDATE_RESULT_SUCCESS = 1
UPDATE_RESULT_NOT_ENOUGH_SPACE = 2
UPDATE_RESULT_OUT_OF_MEMORY = 3
UPDATE_RESULT_CONNECTION_LOST = 4
UPDATE_RESULT_INTEGRITY_FAILURE = 5
UPDATE_RESULT_UNSUPPORTED_PACKAGE_TYPE = 6
UPDATE_RESULT_INVALID_URI = 7
UPDATE_RESULT_FAILED = 8
UPDATE_RESULT_UNSUPPORTED_PROTOCOL = 9

FIRMWARE_PATH = '/firmware'


class FirmwareUpdate:
    class Test(jni_test.LocalSingleServerTest):
        def set_expect_send_after_state_machine_reset(self, expect_send_after_state_machine_reset):
            self.expect_send_after_state_machine_reset = expect_send_after_state_machine_reset

        def setUp(self, garbage=0, *args, **kwargs):
            garbage_lines = ''
            while garbage > 0:
                garbage_line = '#' * (min(garbage, 80) - 1) + '\n'
                garbage_lines += garbage_line
                garbage -= len(garbage_line)
            self.FIRMWARE_SCRIPT_CONTENT = garbage_lines.encode('ascii')
            super().setUp(*args, **kwargs)

        def tearDown(self):
            try:
                super().tearDown()
            finally:
                try:
                    os.unlink('downloaded_firmware')
                except FileNotFoundError:
                    pass

        def read_update_result(self):
            req = Lwm2mRead(ResPath.FirmwareUpdate.UpdateResult)
            self.serv.send(req)
            res = self.serv.recv()
            self.assertMsgEqual(Lwm2mContent.matching(req)(), res)
            return int(res.content)

        def read_state(self):
            req = Lwm2mRead(ResPath.FirmwareUpdate.State)
            self.serv.send(req)
            res = self.serv.recv()
            self.assertMsgEqual(Lwm2mContent.matching(req)(), res)
            return int(res.content)

        def write_firmware_and_wait_for_download(self, firmware_uri: str,
                                                 download_timeout_s=20):
            # Write /5/0/1 (Firmware URI)
            req = Lwm2mWrite(ResPath.FirmwareUpdate.PackageURI, firmware_uri)
            self.serv.send(req)
            self.assertMsgEqual(Lwm2mChanged.matching(req)(),
                                self.serv.recv())

            # wait until client downloads the firmware
            deadline = time.time() + download_timeout_s
            while time.time() < deadline:
                time.sleep(0.5)

                if self.read_state() == UPDATE_STATE_DOWNLOADED:
                    return

            self.fail('firmware still not downloaded')

    class TestWithHttpServer(Test):
        RESPONSE_DELAY = 0
        CHUNK_SIZE = sys.maxsize
        ETAGS = False

        def get_firmware_uri(self):
            return 'http://127.0.0.1:%d%s' % (self.http_server.server_address[1], FIRMWARE_PATH)

        def provide_response(self, use_real_app=False):
            with self._response_cv:
                self.assertIsNone(self._response_content)
                if use_real_app:
                    with open(os.path.join(self.config.demo_path, self.config.demo_cmd), 'rb') as f:
                        firmware = f.read()
                        self._response_content = make_firmware_package(
                            firmware)
                else:
                    self._response_content = make_firmware_package(
                        self.FIRMWARE_SCRIPT_CONTENT)
                self._response_cv.notify()

        def _create_server(self):
            test_case = self

            class FirmwareRequestHandler(http.server.BaseHTTPRequestHandler):
                def do_GET(self):
                    self.send_response(http.HTTPStatus.OK)
                    self.send_header('Content-type', 'text/plain')
                    if test_case.ETAGS:
                        self.send_header('ETag', '"some_etag"')
                    self.end_headers()

                    # This condition variable makes it possible to defer sending the response.
                    # FirmwareUpdateStateChangeTest uses it to ensure demo has enough time
                    # to send the interim "Downloading" state notification.
                    with test_case._response_cv:
                        while test_case._response_content is None:
                            test_case._response_cv.wait()
                        response_content = test_case._response_content
                        test_case.requests.append(self.path)
                        test_case._response_content = None

                    def chunks(data):
                        for i in range(0, len(response_content), test_case.CHUNK_SIZE):
                            yield response_content[i:i + test_case.CHUNK_SIZE]

                    for chunk in chunks(response_content):
                        time.sleep(test_case.RESPONSE_DELAY)
                        self.wfile.write(chunk)
                        self.wfile.flush()

                def log_request(self, code='-', size='-'):
                    # don't display logs on successful request
                    pass

            return http.server.HTTPServer(('', 0), FirmwareRequestHandler)

        def write_firmware_and_wait_for_download(self, *args, **kwargs):
            requests = list(self.requests)
            super().write_firmware_and_wait_for_download(*args, **kwargs)
            self.assertEqual(requests + ['/firmware'], self.requests)

        def setUp(self, *args, **kwargs):
            self.requests = []
            self._response_content = None
            self._response_cv = threading.Condition()

            self.http_server = self._create_server()

            super().setUp(*args, **kwargs)

            self.server_thread = threading.Thread(
                target=lambda: self.http_server.serve_forever())
            self.server_thread.start()

        def tearDown(self):
            try:
                super().tearDown()
            finally:
                self.http_server.shutdown()
                self.server_thread.join()

    class TestWithTlsServer(Test):
        @staticmethod
        def _generate_pem_cert_and_key(cn='127.0.0.1', alt_ip='127.0.0.1'):
            import datetime
            import ipaddress
            from cryptography import x509
            from cryptography.x509.oid import NameOID
            from cryptography.hazmat.backends import default_backend
            from cryptography.hazmat.primitives import hashes, serialization
            from cryptography.hazmat.primitives.asymmetric import rsa

            key = rsa.generate_private_key(public_exponent=65537, key_size=2048,
                                           backend=default_backend())
            name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, cn)])
            now = datetime.datetime.utcnow()
            cert_builder = (x509.CertificateBuilder().
                            subject_name(name).
                            issuer_name(name).
                            public_key(key.public_key()).
                            serial_number(1000).
                            not_valid_before(now).
                            not_valid_after(now + datetime.timedelta(days=1)))
            if alt_ip is not None:
                cert_builder = cert_builder.add_extension(x509.SubjectAlternativeName(
                    [x509.DNSName(cn), x509.IPAddress(ipaddress.IPv4Address(alt_ip))]),
                    critical=False)
            cert = cert_builder.sign(key, hashes.SHA256(), default_backend())
            cert_pem = cert.public_bytes(encoding=serialization.Encoding.PEM)
            key_pem = key.private_bytes(encoding=serialization.Encoding.PEM,
                                        format=serialization.PrivateFormat.TraditionalOpenSSL,
                                        encryption_algorithm=serialization.NoEncryption())
            return cert_pem, key_pem

        def setUp(self, pass_cert_to_demo=True):
            cert_pem, key_pem = self._generate_pem_cert_and_key()

            with tempfile.NamedTemporaryFile(delete=False) as cert_file, \
                    tempfile.NamedTemporaryFile(delete=False) as key_file:
                cert_file.write(cert_pem)
                cert_file.flush()

                key_file.write(key_pem)
                key_file.flush()

                self._cert_file = cert_file.name
                self._key_file = key_file.name

            extra_cmdline_args = []
            if pass_cert_to_demo:
                extra_cmdline_args += ['--fw-cert-file', self._cert_file]
            super().setUp(extra_cmdline_args=extra_cmdline_args)

        def tearDown(self):
            def unlink_without_err(fname):
                try:
                    os.unlink(fname)
                except:
                    print('unlink(%r) failed' % (fname,))
                    sys.excepthook(*sys.exc_info())

            try:
                super().tearDown()
            finally:
                unlink_without_err(self._cert_file)
                unlink_without_err(self._key_file)

    class TestWithHttpsServer(TestWithTlsServer, TestWithHttpServer):
        def get_firmware_uri(self):
            http_uri = super().get_firmware_uri()
            assert http_uri[:5] == 'http:'
            return 'https:' + http_uri[5:]

        def _create_server(self):
            http_server = super()._create_server()
            http_server.socket = ssl.wrap_socket(http_server.socket, certfile=self._cert_file,
                                                 keyfile=self._key_file,
                                                 server_side=True)
            return http_server


class FirmwareUpdateHttpsTest(FirmwareUpdate.TestWithHttpsServer):
    def runTest(self):
        self.provide_response()
        self.write_firmware_and_wait_for_download(
            self.get_firmware_uri(), download_timeout_s=20)

        # Execute /5/0/2 (Update)
        req = Lwm2mExecute(ResPath.FirmwareUpdate.Update)
        self.serv.send(req)
        self.assertMsgEqual(Lwm2mChanged.matching(req)(),
                            self.serv.recv())


class FirmwareUpdateUnconfiguredHttpsTest(FirmwareUpdate.TestWithHttpsServer):
    def setUp(self):
        super().setUp(pass_cert_to_demo=False)

    def runTest(self):
        # disable minimum notification period
        write_attrs_req = Lwm2mWriteAttributes(ResPath.FirmwareUpdate.UpdateResult,
                                               query=['pmin=0'])
        self.serv.send(write_attrs_req)
        self.assertMsgEqual(Lwm2mChanged.matching(
            write_attrs_req)(), self.serv.recv())

        # initial result should be 0
        observe_req = Lwm2mObserve(ResPath.FirmwareUpdate.UpdateResult)
        self.serv.send(observe_req)
        self.assertMsgEqual(Lwm2mContent.matching(
            observe_req)(content=b'0'), self.serv.recv())

        # Write /5/0/1 (Firmware URI)
        req = Lwm2mWrite(ResPath.FirmwareUpdate.PackageURI,
                         self.get_firmware_uri())
        self.serv.send(req)
        self.assertMsgEqual(Lwm2mChanged.matching(req)(),
                            self.serv.recv())

        # even before reaching the server, we should get an error
        notify_msg = self.serv.recv()
        # no security information => "Unsupported protocol"
        self.assertMsgEqual(Lwm2mNotify(observe_req.token,
                                        str(UPDATE_RESULT_UNSUPPORTED_PROTOCOL).encode()),
                            notify_msg)
        self.serv.send(Lwm2mReset(msg_id=notify_msg.msg_id))
        self.assertEqual(0, self.read_state())
