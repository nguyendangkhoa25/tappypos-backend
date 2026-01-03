# UTF-8 Encoding Configuration for i18n

## ✅ Files Now Use UTF-8 Encoding

The `messages_vi.properties` file now uses **UTF-8 encoding** with readable Vietnamese characters instead of Unicode escape sequences.

---

## 🔧 IDE Configuration

### IntelliJ IDEA:

1. **File → Settings → Editor → File Encodings**
2. Set:
   - **Global Encoding**: UTF-8
   - **Project Encoding**: UTF-8
   - **Default encoding for properties files**: UTF-8
3. Check: ☑ **Transparent native-to-ascii conversion**

### VS Code:

1. **File → Preferences → Settings**
2. Search for "encoding"
3. Set **Files: Encoding** to `UTF-8`
4. Add to `.vscode/settings.json`:
```json
{
  "files.encoding": "utf8",
  "[properties]": {
    "files.encoding": "utf8"
  }
}
```

### Eclipse:

1. **Window → Preferences → General → Workspace**
2. Set **Text file encoding** to `UTF-8`
3. **Window → Preferences → General → Content Types**
4. Select **Text → Java Properties File**
5. Set **Default encoding** to `UTF-8`

---

## 📝 Maven Configuration

The `pom.xml` includes:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <encoding>UTF-8</encoding>
        <propertiesEncoding>UTF-8</propertiesEncoding>
    </configuration>
</plugin>
```

This ensures properties files are read and compiled with UTF-8 encoding.

---

## ✨ Benefits

**Before (Unicode Escapes):**
```properties
error.user.duplicate.username=T\u00ean \u0111\u0103ng nh\u1eadp \u0111\u00e3 t\u1ed3n t\u1ea1i: {0}
```
❌ Hard to read
❌ Hard to edit
❌ Error-prone

**After (UTF-8):**
```properties
error.user.duplicate.username=Tên đăng nhập đã tồn tại: {0}
```
✅ Easy to read
✅ Easy to edit
✅ Developer-friendly

---

## 🧪 Testing

After configuration, you should be able to:

1. ✅ See Vietnamese characters correctly in your IDE
2. ✅ Edit Vietnamese text directly without escaping
3. ✅ Build the project successfully with `mvn clean package`
4. ✅ Run the application and see correct Vietnamese messages

---

## ⚠️ Important Notes

1. **Save files as UTF-8**: Always ensure your IDE saves `.properties` files with UTF-8 encoding
2. **Git**: The files will be committed as UTF-8 (no conversion needed)
3. **Team**: All developers should use UTF-8 encoding in their IDEs

---

## 🔍 Troubleshooting

**Problem**: Characters still show as `?` or garbled

**Solutions**:
1. Check IDE encoding settings (see above)
2. Check file encoding: `file -I messages_vi.properties` (should show `utf-8`)
3. Re-save file with UTF-8 encoding
4. Rebuild project: `mvn clean package`
5. Restart IDE

**Problem**: Build fails with encoding errors

**Solution**:
```bash
mvn clean package -Dproject.build.sourceEncoding=UTF-8
```

---

## 📚 Additional Resources

- [Spring Boot i18n Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.internationalization)
- [Maven Resources Plugin](https://maven.apache.org/plugins/maven-resources-plugin/)
- [Java Properties File Encoding](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html)

---

**Enjoy readable Vietnamese messages! 🇻🇳**

