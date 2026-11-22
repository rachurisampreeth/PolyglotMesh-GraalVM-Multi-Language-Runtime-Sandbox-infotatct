package com.polyglotmesh.service;

import com.polyglotmesh.api.ApiModels.Template;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class TemplateCatalog {
    public List<Template> getTemplates() {
        Map<String,Object> commerce = Map.of("product", "Starter Plan", "unitPrice", 1499,
            "quantity", 3, "coupon", "SAVE10");
        return List.of(
            new Template("pricing-engine", "Dynamic pricing engine", "Java → Python → JavaScript", "pipeline", "python", "",
                "# The Java host makes input_data available in GraalPy\n"
                + "subtotal = input_data['unitPrice'] * input_data['quantity']\n"
                + "discount = 0.10 if input_data['coupon'] == 'SAVE10' else 0\n"
                + "result = round(subtotal * (1 - discount), 2)\n"
                + "print('Python pricing result: INR', result)\n",
                "// Consume the Python result without a network request\n"
                + "const payable = Number(python_result);\n"
                + "const approved = payable > 0 && payable < 100000;\n"
                + "result = JSON.stringify({\n"
                + "  approved, payable, currency: 'INR',\n"
                + "  source: 'GraalPy → GraalJS'\n"
                + "});\n"
                + "print('JS fraud-check approved:', approved);\n", commerce),
            new Template("python-hello", "Python data processing", "GraalPy single language", "single", "python",
                "# input_data comes from a Java Map\n"
                + "values = [10, 20, 30, 40]\n"
                + "result = sum(values) / len(values)\n"
                + "print('Computed mean:', result)\n", "", "", commerce),
            new Template("javascript-hello", "JavaScript validation", "GraalJS single language", "single", "javascript",
                "const price = Number(input_data.unitPrice);\n"
                + "const qty = Number(input_data.quantity);\n"
                + "result = JSON.stringify({subtotal: price * qty, valid: qty > 0});\n"
                + "print('Calculated in JavaScript:', price * qty);\n", "", "", commerce),
            new Template("blocked-access", "Permission check", "Demonstrate restricted host access", "single", "javascript",
                "// Java host classes are deliberately not available.\n"
                + "// This should fail instead of accessing java.lang.System.\n"
                + "result = Java.type('java.lang.System').getProperty('user.home');\n", "", "", commerce)
        );
    }
}
