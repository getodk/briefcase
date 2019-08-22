/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

public class InMemoryPreferences extends Preferences {
  private final Map<String, String> storage;

  public InMemoryPreferences(Map<String, String> storage) {
    this.storage = storage;
  }

  public static Preferences empty() {
    return new InMemoryPreferences(new ConcurrentHashMap<>());
  }

  @Override
  public void put(String key, String value) {
    storage.put(key, value);
  }

  private Optional<String> nullSafeGet(String key) {
    return Optional.ofNullable(storage.get(key));
  }

  @Override
  public String get(String key, String def) {
    return nullSafeGet(key).orElse(def);
  }

  @Override
  public void remove(String key) {
    storage.remove(key);
  }

  @Override
  public void clear() throws BackingStoreException {
    storage.clear();
  }

  @Override
  public void putInt(String key, int value) {
    storage.put(key, Integer.valueOf(value).toString());
  }

  @Override
  public int getInt(String key, int def) {
    return nullSafeGet(key).map(Integer::parseInt).orElse(def);
  }

  @Override
  public long getLong(String key, long def) {
    return nullSafeGet(key).map(Long::parseLong).orElse(def);
  }

  @Override
  public boolean getBoolean(String key, boolean def) {
    return nullSafeGet(key).map(Boolean::parseBoolean).orElse(def);
  }

  @Override
  public float getFloat(String key, float def) {
    return nullSafeGet(key).map(Float::parseFloat).orElse(def);
  }

  @Override
  public double getDouble(String key, double def) {
    return nullSafeGet(key).map(Double::parseDouble).orElse(def);
  }

  @Override
  public byte[] getByteArray(String key, byte[] def) {
    return nullSafeGet(key).map(String::getBytes).orElse(def);
  }

  @Override
  public void putLong(String key, long value) {
    storage.put(key, Long.valueOf(value).toString());
  }

  @Override
  public void putBoolean(String key, boolean value) {
    storage.put(key, Boolean.valueOf(value).toString());
  }

  @Override
  public void putFloat(String key, float value) {
    storage.put(key, Float.valueOf(value).toString());
  }

  @Override
  public void putDouble(String key, double value) {
    storage.put(key, Double.valueOf(value).toString());
  }

  @Override
  public void putByteArray(String key, byte[] value) {
    storage.put(key, new String(value));
  }

  @Override
  public String[] keys() throws BackingStoreException {
    return storage.keySet().toArray(new String[storage.size()]);
  }

  @Override
  public String[] childrenNames() throws BackingStoreException {
    return storage.keySet().toArray(new String[storage.size()]);
  }

  @Override
  public Preferences parent() {
    return null;
  }

  @Override
  public Preferences node(String pathName) {
    return null;
  }

  @Override
  public boolean nodeExists(String pathName) throws BackingStoreException {
    return false;
  }

  @Override
  public void removeNode() throws BackingStoreException {

  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public String absolutePath() {
    return null;
  }

  @Override
  public boolean isUserNode() {
    return false;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public void flush() throws BackingStoreException {

  }

  @Override
  public void sync() throws BackingStoreException {

  }

  @Override
  public void addPreferenceChangeListener(PreferenceChangeListener pcl) {

  }

  @Override
  public void removePreferenceChangeListener(PreferenceChangeListener pcl) {

  }

  @Override
  public void addNodeChangeListener(NodeChangeListener ncl) {

  }

  @Override
  public void removeNodeChangeListener(NodeChangeListener ncl) {

  }

  @Override
  public void exportNode(OutputStream os) throws IOException, BackingStoreException {

  }

  @Override
  public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {

  }
}
