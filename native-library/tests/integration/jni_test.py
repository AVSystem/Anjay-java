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

from framework.test_suite import *


class LocalSingleServerTest(test_suite.Lwm2mSingleServerTest):
    def _start_demo(self, cmdline_args, timeout_s=5):
        """
        Starts the demo executable with given CMDLINE_ARGS.
        """

        def filter_args(args):
            result = []
            i = 0
            while i < len(args):
                if args[i] in {'--ciphersuites', '--fw-updated-marker-path'}:
                    i += 2
                else:
                    result.append(args[i])
                    i += 1
            return result

        demo_executable = os.path.join(
            self.config.demo_path, self.config.demo_cmd)

        args_prefix = []
        if (os.environ.get('RR')
                or ('RRR' in os.environ
                    and test_or_suite_matches_query_regex(self, os.environ['RRR']))):
            logging.info('*** rr-recording enabled ***')
            args_prefix = ['rr', 'record']

        library_path = os.path.abspath(os.path.dirname(demo_executable))
        demo_args = args_prefix + \
                    ['java', '-Djava.library.path=%s' % library_path, '-jar', demo_executable] + \
                    filter_args(cmdline_args)

        import shlex
        console_log_path = self.logs_path(LogType.Console)
        console = open(console_log_path, 'w')
        console.write(
            (' '.join(map(shlex.quote, demo_args)) + '\n\n'))
        console.flush()

        logging.debug('starting demo: %s', ' '.join(
            '"%s"' % arg for arg in demo_args))
        import subprocess
        self.demo_process = subprocess.Popen(demo_args,
                                             stdin=subprocess.PIPE,
                                             stdout=console,
                                             stderr=console,
                                             bufsize=0)
        self.demo_process.log_file_write = console
        self.demo_process.log_file_path = console_log_path
        self.demo_process.log_file = open(
            console_log_path, mode='rb', buffering=0)

        if timeout_s is not None:
            # wait until demo process starts
            if self.read_log_until_match(regex=re.escape(
                                             b'*** DEMO STARTUP FINISHED ***'),
                                         timeout_s=timeout_s) is None:
                raise self.failureException(
                    'demo executable did not start in time')
