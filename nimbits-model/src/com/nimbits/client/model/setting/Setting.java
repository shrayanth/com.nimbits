/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.client.model.setting;

import com.nimbits.client.enums.SettingType;

import java.io.*;

/**
 * Created by Benjamin Sautner
 * User: benjamin
 * Date: 5/5/11
 * Time: 3:41 PM
 */
public interface Setting extends Serializable {

    SettingType getSetting();

    String getValue();

    void setValue(String newValue);
}
