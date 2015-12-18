/*
 * SonarQube :: Plugins :: SCM :: Perforce
 * Copyright (C) 2014 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.scm.perforce;

import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.scm.BlameLine;

/**
 * This class handles the result from the Perforce annotate and revision history commands and constructs blame lines
 * for SonarQube.
 */
public class PerforceBlameResult {

  private static final Logger LOG = LoggerFactory.getLogger(PerforceBlameResult.class);

  /**
   * Change lists
   */
  private List<String> changeLists = new ArrayList<>();

  /** The dates. */
  private Map<String, Date> dates = new HashMap<>();

  /** The authors. */
  private Map<String, String> authors = new HashMap<>();

  /**
   * Extracts file annotation info.
   *
   * @param fileAnnotations
   *            the file annotations
   */
  public void processBlameLines(List<IFileAnnotation> fileAnnotations) {
    if (fileAnnotations != null) {
      for (IFileAnnotation fileAnnotation : fileAnnotations) {
        if (fileAnnotation != null) {
          changeLists.add(String.valueOf(fileAnnotation.getLower()));
        }
      }
    }
  }

  /**
   * Extracts dates, authors and revision number from revision history map.
   *
   * @param revisionMap
   *            the revision map
   */
  public void processRevisionHistory(Map<IFileSpec, List<IFileRevisionData>> revisionMap) {
    if (revisionMap != null) {
      for (Map.Entry<IFileSpec, List<IFileRevisionData>> entry : revisionMap.entrySet()) {
        List<IFileRevisionData> changes = entry.getValue();
        if (changes != null) {
          for (IFileRevisionData change : changes) {
            dates.put(String.valueOf(change.getChangelistId()), change.getDate());
            authors.put(String.valueOf(change.getChangelistId()), change.getUserName());
          }
        }
      }
    }
  }

  /**
   * Combine results of annotation and revision history commands and return blame lines.
   * @return blame lines with revision date and author fields filled.
   */
  public List<BlameLine> createBlameLines() {
    List<BlameLine> lines = new ArrayList<>(changeLists.size() + 1);

    for (String changeList : changeLists) {
      BlameLine line = new BlameLine();
      line.revision(changeList);
      Date date = dates.get(changeList);
      if (date == null) {
        LOG.warn("Unable to find date for changeset #" + changeList);
        continue;
      }
      line.revision(changeList);
      line.date(date);
      line.author(authors.get(changeList));
      lines.add(line);
    }

    return lines;
  }

}
