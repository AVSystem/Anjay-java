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
        IncrementInt = 10
