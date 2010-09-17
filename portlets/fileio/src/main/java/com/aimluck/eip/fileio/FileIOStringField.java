/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2010 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.fileio;

import java.util.List;

import com.aimluck.commons.field.ALStringField;

/**
 * $Id$
 */
public class FileIOStringField extends ALStringField {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private boolean validate = true;

  public FileIOStringField() {
    super();
  }

  public FileIOStringField(String string) {
    super(string);
  }

  @Override
  public boolean validate(List<String> msgList) {
    validate = super.validate(msgList);
    return validate;
  }

  public void setValidate(boolean bool) {
    validate = bool;
  }

  public boolean getValidate() {
    return validate;
  }
}
