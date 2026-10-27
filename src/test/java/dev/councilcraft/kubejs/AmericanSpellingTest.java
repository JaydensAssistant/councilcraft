package dev.councilcraft.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.ast.AstRoot;
import dev.latvian.mods.rhino.ast.StringLiteral;
import dev.latvian.mods.rhino.ast.TemplateCharacters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * PT47: all player-facing text CouncilCraft writes uses standard American spelling. This scans en_us.json (ours and the
 * KubeJS ones), KubeJS string literals, the generated advancement JSON, the tips table and the credits files for common
 * British spellings, and fails with the file and line. Code comments and identifiers are out of scope, as are other mods'
 * own text and license titles (see ALLOW).
 */
class AmericanSpellingTest {
    /** Common British spellings. Stems end in a group so the matching plural, past and -ing forms are caught too. */
    static final Pattern BRITISH = Pattern.compile("\\b(?:"
            + "colou?rs?(?<=colours?)|colou?red(?<=coloured)|colouring|colourful|"
            + "behaviou?rs?(?<=behaviours?)|behavioural|"
            + "centres?|centred|centring|"
            + "armour\\w*|favour\\w*|flavour\\w*|honour\\w*|labour\\w*|neighbour\\w*|humour\\w*|rumour\\w*|odour\\w*|vapour\\w*|savour\\w*|endeavour\\w*|harbour\\w*|"
            + "gre[yi]s?(?<=greys?)|greyed|greying|"
            + "(?:organi|recogni|reali|customi|optimi|minimi|maximi|summari|finali|apologi|categori|synchroni|initiali|utili|authori|stabili|normali|speciali|prioriti|visuali|"
            + "personali|memori|criticiz|emphasi|capitali|civili|colonia|digiti|energi|fertili|generali|hospitali|idealogi|legali|materiali|neutrali|penali|publici|sanitis|"
            + "sensiti|standardi|subsidi|symboli|terrori|vandali)s(?:e|es|ed|ing|ation|ations)|"
            + "travell(?:ed|er|ers|ing)|modell(?:ed|ing)|labell(?:ed|ing)|signall(?:ed|ing)|fuell(?:ed|ing)|levell(?:ed|ing)|jewell?e?ry|jewelled|"
            + "licen[cs]e?s?(?<=licences?)|defences?|offences?|pretences?|whilst|amongst|learnt|spelt|dreamt|burnt|programmes?|catalogue[sd]?|"
            + "tyres?|kerbs?|ploughs?|moulds?|moulded|sceptres?|theatres?|metres?|litres?|fibres?|calibre|lustre|sabre|aluminium|sulphur|practise[sd]?|"
            + "cosy|draughts?|storeys?|tonnes?|pyjamas|artefacts?|manoeuvres?|anticlockwise|fulfil|skilful|grey\\w*|mum|maths|lorry|petrol|bonnet|boot of"
            + ")\\b", Pattern.CASE_INSENSITIVE);

    /** Entries are file-suffix + ":" + the exact matched word, lower case. Proper names and license titles only. */
    static final List<String> ALLOW = List.of();

    @Test
    void playerFacingTextIsAmerican() throws IOException {
        List<String> hits = new ArrayList<>();
        // 1. lang files: ours (jar) and the KubeJS ones
        List<Path> langs = new ArrayList<>(List.of(Path.of("src/main/resources/assets/councilcraft/lang/en_us.json")));
        try (Stream<Path> s = Files.walk(RhinoKubeJs.KUBEJS.resolve("assets"))) {
            s.filter(p -> p.toString().contains("/lang/") && p.toString().endsWith(".json")).forEach(langs::add);
        }
        for (Path p : langs) {
            scanJson(p, hits);
        }
        // 2. generated advancement JSON
        try (Stream<Path> s = Files.walk(Path.of("pack/moonlight-global-datapacks"))) {
            for (Path p : s.filter(p -> p.toString().contains("/advancements/") && p.toString().endsWith(".json")).toList()) {
                scanJson(p, hits);
            }
        }
        // 3. KubeJS string literals (tips, chat messages, tooltips, MOTD...)
        for (Path p : RhinoKubeJs.kubejsFiles()) {
            for (StringAt lit : stringLiterals(RhinoKubeJs.read(p), p.toString())) {
                scanText(lit.text, p + ":" + lit.line, hits);
            }
        }
        // 4. tips table (only the tip text cell; the other columns are builder notes) and credits files, line by line
        List<String> tipLines = Files.readAllLines(Path.of("docs/tips/tips-locked-PT41.md"));
        for (int i = 0; i < tipLines.size(); i++) {
            String[] cells = tipLines.get(i).split("\\|");
            if (cells.length > 2 && cells[1].trim().matches("\\d+")) {
                scanText(cells[2], "docs/tips/tips-locked-PT41.md:" + (i + 1), hits);
            }
        }
        for (Path p : List.of(Path.of("pack/credits.txt"),
                Path.of("src/main/resources/META-INF/councilcraft-texture-credits.txt"))) {
            List<String> lines = Files.readAllLines(p);
            for (int i = 0; i < lines.size(); i++) {
                scanText(lines.get(i), p + ":" + (i + 1), hits);
            }
        }
        assertEquals(List.of(), hits, "British spelling in player-facing text: use the American form (or add a proper name to ALLOW)");
    }

    @Test
    void scannerCatchesTheKnownSlips() {
        for (String bad : List.of("centred on spawn", "For colour prints", "Colour film", "colours", "A favourite armour", "your grey wool",
                "it organises things", "whilst travelling", "The licence", "a defence", "the neighbours")) {
            List<String> hits = new ArrayList<>();
            scanText(bad, "t:1", hits);
            assertFalse(hits.isEmpty(), bad);
        }
        for (String ok : List.of("centered on spawn", "color prints", "Color film", "gray wool", "License: MIT", "a defense", "rise and exercise",
                "the otherwise surprising advertise", "metal color", "Behavior", "armor stand", "plow", "mold")) {
            List<String> hits = new ArrayList<>();
            scanText(ok, "t:1", hits);
            assertTrue(hits.isEmpty(), ok + " -> " + hits);
        }
    }

    private static void scanText(String text, String where, List<String> hits) {
        Matcher m = BRITISH.matcher(text);
        while (m.find()) {
            String word = m.group().toLowerCase();
            boolean allowed = ALLOW.stream().anyMatch(a -> a.equals(where.replaceAll(":\\d+$", "").replaceAll(".*/", "") + ":" + word) || a.equals("*:" + word));
            if (!allowed) {
                hits.add(where + " \"" + word + "\" in: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text));
            }
        }
    }

    private static void scanJson(Path p, List<String> hits) throws IOException {
        List<String> lines = Files.readAllLines(p);
        collectJson(JsonParser.parseString(String.join("\n", lines)), p, lines, hits);
    }

    private static void collectJson(JsonElement e, Path p, List<String> lines, List<String> hits) {
        if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(en -> collectJson(en.getValue(), p, lines, hits));
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> collectJson(x, p, lines, hits));
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String v = e.getAsString();
            Matcher m = BRITISH.matcher(v);
            if (m.find()) {
                int line = 0;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains(v.length() > 30 ? v.substring(0, 30) : v)) {
                        line = i + 1;
                        break;
                    }
                }
                scanText(v, p + ":" + line, hits);
            }
        }
    }

    record StringAt(String text, int line) {}

    static List<StringAt> stringLiterals(String src, String name) {
        Context cx = Context.enter();
        CompilerEnvirons env = new CompilerEnvirons();
        env.initFromContext(cx);
        AstRoot root = new Parser(cx, env).parse(src, name, 1);
        List<StringAt> out = new ArrayList<>();
        for (Object n : RhinoKubeJs.walk(root)) {
            if (n instanceof StringLiteral s) {
                out.add(new StringAt(s.getValue(), s.getLineno()));
            } else if (n instanceof TemplateCharacters t) {
                out.add(new StringAt(t.getValue(), t.getLineno()));
            }
        }
        return out;
    }
}
