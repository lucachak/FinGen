import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';
import 'package:fingen_mobile/shared/widgets/zen_card.dart';
import 'package:fingen_mobile/features/ai_assistant/data/ai_repository.dart';

class AiChatScreen extends ConsumerStatefulWidget {
  const AiChatScreen({super.key});

  @override
  ConsumerState<AiChatScreen> createState() => _AiChatScreenState();
}

class _AiChatScreenState extends ConsumerState<AiChatScreen> {
  final _messageController = TextEditingController();
  final List<Map<String, dynamic>> _messages = [
    {
      'text': 'Olá! Sou o seu Assistente IA do FinGen. Como posso ajudar com suas finanças hoje?',
      'isUser': false
    }
  ];
  bool _isLoading = false;

  @override
  void dispose() {
    _messageController.dispose();
    super.dispose();
  }

  void _onSend() async {
    final text = _messageController.text.trim();
    if (text.isEmpty) return;

    setState(() {
      _messages.add({'text': text, 'isUser': true});
      _isLoading = true;
    });
    _messageController.clear();

    try {
      final response = await ref.read(aiRepositoryProvider).getChatResponse(text);
      setState(() {
        _messages.add({'text': response, 'isUser': false});
      });
    } catch (e) {
      setState(() {
        _messages.add({'text': 'Desculpe, tive um problema ao processar sua solicitação. Tente novamente.', 'isUser': false});
      });
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = FinGenTheme.colors(context);

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'Assistente IA',
          style: GoogleFonts.notoSerifJp(
            fontWeight: FontWeight.bold,
            color: colors.text1,
          ),
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                return _buildMessage(msg['text'], msg['isUser']);
              },
            ),
          ),
          if (_isLoading)
            Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  const SizedBox(
                    width: 12,
                    height: 12,
                    child: CircularProgressIndicator(strokeWidth: 2, color: FinGenTheme.brand),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    'Pensando...',
                    style: GoogleFonts.dmSans(fontSize: 12, color: colors.text3, fontStyle: FontStyle.italic),
                  ),
                ],
              ),
            ),
          _buildInput(context, colors),
        ],
      ),
    );
  }

  Widget _buildMessage(String text, bool isUser) {
    final colors = FinGenTheme.colors(context);
    
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: ZenCard(
          padding: const EdgeInsets.all(16),
          color: isUser ? FinGenTheme.brand : Theme.of(context).cardTheme.color,
          shadowSize: ZenShadowSize.sm,
          borderRadius: 20,
          border: isUser ? BorderSide.none : BorderSide(color: colors.border),
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.75),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (!isUser)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        const Icon(LucideIcons.sparkles, size: 12, color: FinGenTheme.brand),
                        const SizedBox(width: 6),
                        Text(
                          'FINANCIAL BRAIN',
                          style: GoogleFonts.dmSans(
                            fontSize: 9,
                            fontWeight: FontWeight.bold,
                            color: FinGenTheme.brand,
                            letterSpacing: 1,
                          ),
                        ),
                      ],
                    ),
                  ),
                Text(
                  text,
                  style: GoogleFonts.dmSans(
                    color: isUser ? Colors.white : colors.text1,
                    fontSize: 14,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInput(BuildContext context, FinGenColors colors) {
    return Container(
      padding: EdgeInsets.fromLTRB(20, 12, 20, MediaQuery.of(context).padding.bottom + 12),
      decoration: BoxDecoration(
        color: Theme.of(context).cardTheme.color,
        border: Border(top: BorderSide(color: colors.border)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: colors.surface2,
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: colors.border),
              ),
              child: TextField(
                controller: _messageController,
                style: GoogleFonts.dmSans(color: colors.text1, fontSize: 14),
                decoration: const InputDecoration(
                  hintText: 'Pergunte sobre suas finanças...',
                  border: InputBorder.none,
                  enabledBorder: InputBorder.none,
                  focusedBorder: InputBorder.none,
                  contentPadding: EdgeInsets.symmetric(vertical: 12),
                ),
                onSubmitted: (_) => _onSend(),
              ),
            ),
          ),
          const SizedBox(width: 8),
          CircleAvatar(
            backgroundColor: FinGenTheme.brand,
            radius: 22,
            child: IconButton(
              icon: const Icon(LucideIcons.send, color: Colors.white, size: 18),
              onPressed: _onSend,
            ),
          ),
        ],
      ),
    );
  }
}
