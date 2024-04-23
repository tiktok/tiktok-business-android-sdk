/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.model;

public class Property {
    public String _id;
    public String key;
    public String value;

    public Property(String id, String key, String value) {
        this._id = id;
        this.key = key;
        this.value = value;
    }
}
